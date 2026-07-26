package com.example.LMS_BE.student.service;

import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.common.exception.ResourceNotFoundException;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.entity.MediaFile;
import com.example.LMS_BE.media.service.MediaService;
import com.example.LMS_BE.student.dto.StudentRequest;
import com.example.LMS_BE.student.dto.StudentResponse;
import com.example.LMS_BE.student.enity.Student;
import com.example.LMS_BE.student.mapper.StudentMapper;
import com.example.LMS_BE.student.repository.StudentRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StudentService {
  private final StudentRepository repo;
  private final StudentMapper mapper;
  private final MediaService media;

  public PageResponse<StudentResponse> search(String keyword, int page, int size) {
    Page<Student> result =
        repo.search(
            normalize(keyword), PageRequest.of(page, size, Sort.by("createdAt").descending()));
    Map<Long, List<MediaResponse>> mediaMap =
        media.findByObjects(ObjectType.STUDENT, ids(result.getContent()));
    return PageResponse.from(result.map(x -> mapper.toResponse(x, mediaMap)));
  }

  public StudentResponse detail(Long id) {
    return response(get(id));
  }

  @Transactional
  public StudentResponse create(
      StudentRequest request, MultipartFile avatar, List<MultipartFile> avatars) {
    NormalizedStudent normalized = normalize(request);
    validate(normalized, null);
    Student student = mapper.toEntity(request);
    applyNormalized(student, normalized);
    save(student);
    media.sync(
        ObjectType.STUDENT,
        student.getId(),
        List.of(),
        request.getNewMediaIds(),
        List.of(),
        MediaType.AVATAR);
    MediaFile uploaded = media.store(avatar);
    media.attach(ObjectType.STUDENT, student.getId(), uploaded, MediaType.AVATAR);
    media.attachUploaded(ObjectType.STUDENT, student.getId(), avatars, MediaType.AVATAR);
    return response(student);
  }

  @Transactional
  public StudentResponse update(
      Long id, StudentRequest request, MultipartFile avatar, List<MultipartFile> avatars) {
    Student student = get(id);
    NormalizedStudent normalized = normalize(request);
    validate(normalized, id);
    mapper.updateEntity(request, student);
    applyNormalized(student, normalized);
    save(student);
    media.sync(
        ObjectType.STUDENT,
        id,
        request.getRetainedMediaIds(),
        request.getNewMediaIds(),
        request.getRemovedMediaIds(),
        MediaType.AVATAR);
    media.attach(ObjectType.STUDENT, id, media.store(avatar), MediaType.AVATAR);
    media.attachUploaded(ObjectType.STUDENT, id, avatars, MediaType.AVATAR);
    return response(student);
  }

  @Transactional
  public void delete(Long id) {
    Student student = get(id);
    student.softDelete();
    repo.save(student);
  }

  public byte[] export(String keyword) {
    List<Student> rows = repo.export(normalize(keyword));
    if (rows.isEmpty()) throw new ResourceNotFoundException(ErrorCode.STUDENT_EXPORT_EMPTY);
    try (var wb = new XSSFWorkbook();
        var out = new ByteArrayOutputStream()) {
      var sheet = wb.createSheet("Students");
      String[] headers = {"ID", "Mã học viên", "Họ tên", "Email", "Điện thoại"};
      var head = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) head.createCell(i).setCellValue(headers[i]);
      int row = 1;
      for (Student s : rows) {
        var x = sheet.createRow(row++);
        x.createCell(0).setCellValue(s.getId());
        x.createCell(1).setCellValue(s.getStudentCode());
        x.createCell(2).setCellValue(s.getFullName());
        x.createCell(3).setCellValue(s.getEmail());
        x.createCell(4).setCellValue(Optional.ofNullable(s.getPhone()).orElse(""));
      }
      wb.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.EXCEL_EXPORT_FAILED);
    }
  }

  private StudentResponse response(Student student) {
    return mapper.toResponse(
        student, media.findByObjects(ObjectType.STUDENT, List.of(student.getId())));
  }

  private Student get(Long id) {
    return repo.findByIdAndStatus(id, 1)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));
  }

  private void validate(NormalizedStudent student, Long id) {
    boolean code =
        id == null
            ? repo.existsByStudentCodeIgnoreCaseAndStatus(student.studentCode, 1)
            : repo.existsByStudentCodeIgnoreCaseAndStatusAndIdNot(student.studentCode, 1, id);
    if (code) throw new DuplicateDataException(ErrorCode.STUDENT_DUPLICATE_CODE);

    // Email remains unique even when an old student has status = 0. This mirrors
    // the database unique constraint and prevents a generic constraint error.
    boolean email =
        id == null
            ? repo.existsByEmailIgnoreCase(student.email)
            : repo.existsByEmailIgnoreCaseAndIdNot(student.email, id);
    if (email) throw new DuplicateDataException(ErrorCode.STUDENT_DUPLICATE_EMAIL);
  }

  private void save(Student student) {
    try {
      // Flush here so an email constraint violation is translated while the
      // operation still has student-specific context.
      repo.saveAndFlush(student);
    } catch (DataIntegrityViolationException exception) {
      if (isEmailConstraintViolation(exception))
        throw new DuplicateDataException(ErrorCode.STUDENT_DUPLICATE_EMAIL);
      throw exception;
    }
  }

  private boolean isEmailConstraintViolation(Throwable exception) {
    Throwable current = exception;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("email")
            && (normalized.contains("duplicate") || normalized.contains("unique"))) return true;
      }
      // MariaDB error 1062 is a duplicate unique-key violation. In the
      // intended students schema email is the only globally unique
      // business field; student_code deliberately has no unique index.
      if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 1062)
        return true;
      current = current.getCause();
    }
    return false;
  }

  private NormalizedStudent normalize(StudentRequest request) {
    return new NormalizedStudent(
        request.getStudentCode().trim(),
        request.getFullName().trim(),
        request.getEmail().trim().toLowerCase(Locale.ROOT),
        trimToNull(request.getPhone()),
        trimToNull(request.getGender()),
        trimToNull(request.getAddress()));
  }

  private void applyNormalized(Student student, NormalizedStudent normalized) {
    student.setStudentCode(normalized.studentCode);
    student.setFullName(normalized.fullName);
    student.setEmail(normalized.email);
    student.setPhone(normalized.phone);
    student.setGender(normalized.gender);
    student.setAddress(normalized.address);
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private static final class NormalizedStudent {
    private final String studentCode;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String gender;
    private final String address;

    private NormalizedStudent(
        String studentCode,
        String fullName,
        String email,
        String phone,
        String gender,
        String address) {
      this.studentCode = studentCode;
      this.fullName = fullName;
      this.email = email;
      this.phone = phone;
      this.gender = gender;
      this.address = address;
    }
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private List<Long> ids(List<Student> items) {
    return items.stream().map(Student::getId).toList();
  }
}
