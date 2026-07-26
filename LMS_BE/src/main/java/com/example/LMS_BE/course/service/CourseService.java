package com.example.LMS_BE.course.service;

import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.common.exception.ResourceNotFoundException;
import com.example.LMS_BE.course.dto.CourseRequest;
import com.example.LMS_BE.course.dto.CourseResponse;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.course.mapper.CourseMapper;
import com.example.LMS_BE.course.repository.CourseRepository;
import com.example.LMS_BE.enrollment.repository.EnrollmentRepository;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.service.MediaService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CourseService {

  private static final int ACTIVE_STATUS = 1;

  private final CourseRepository repo;
  private final EnrollmentRepository enrollmentRepo;
  private final CourseMapper mapper;
  private final MediaService media;

  public PageResponse<CourseResponse> search(
      String keyword, LocalDate from, LocalDate to, int page, int size) {
    Page<Course> result =
        repo.search(
            normalizeNullable(keyword),
            from,
            to,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    Map<Long, List<MediaResponse>> mediaMap =
        media.findByObjects(
            ObjectType.COURSE, result.getContent().stream().map(Course::getId).toList());
    return PageResponse.from(result.map(course -> mapper.toResponse(course, mediaMap)));
  }

  public CourseResponse detail(Long id) {
    return response(get(id));
  }

  @Transactional
  public CourseResponse create(
      CourseRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> thumbnails,
      List<MultipartFile> images,
      List<MultipartFile> videos) {
    normalize(request);
    validate(request, null);

    Course course = mapper.toEntity(request);
    repo.save(course);
    media.sync(
        ObjectType.COURSE,
        course.getId(),
        List.of(),
        request.getNewMediaIds(),
        List.of(),
        MediaType.THUMBNAIL);
    media.attach(ObjectType.COURSE, course.getId(), media.store(thumbnail), MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.COURSE, course.getId(), thumbnails, MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.COURSE, course.getId(), images, MediaType.IMAGE);
    media.attachUploaded(ObjectType.COURSE, course.getId(), videos, MediaType.VIDEO);
    return response(course);
  }

  @Transactional
  public CourseResponse update(
      Long id,
      CourseRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> thumbnails,
      List<MultipartFile> images,
      List<MultipartFile> videos) {
    Course course = get(id);
    normalize(request);
    validate(request, id);

    mapper.updateEntity(request, course);
    repo.save(course);
    media.sync(
        ObjectType.COURSE,
        id,
        request.getRetainedMediaIds(),
        request.getNewMediaIds(),
        request.getRemovedMediaIds(),
        MediaType.THUMBNAIL);
    media.attach(ObjectType.COURSE, id, media.store(thumbnail), MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.COURSE, id, thumbnails, MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.COURSE, id, images, MediaType.IMAGE);
    media.attachUploaded(ObjectType.COURSE, id, videos, MediaType.VIDEO);
    return response(course);
  }

  @Transactional
  public void delete(Long id) {
    Course course = get(id);
    if (enrollmentRepo.existsByCourseIdAndStatus(id, ACTIVE_STATUS)) {
      throw new BusinessException(ErrorCode.COURSE_HAS_ACTIVE_ENROLLMENTS);
    }
    course.softDelete();
    repo.save(course);
  }

  public byte[] export(String keyword, LocalDate from, LocalDate to) {
    List<Course> rows = repo.export(normalizeNullable(keyword), from, to);
    if (rows.isEmpty()) {
      throw new ResourceNotFoundException(ErrorCode.COURSE_EXPORT_EMPTY);
    }

    try (var workbook = new XSSFWorkbook();
        var output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Courses");
      String[] headers = {
        "ID", "Mã khóa học", "Tên khóa học", "Giá", "Ngày bắt đầu", "Ngày kết thúc"
      };
      var headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      int rowIndex = 1;
      for (Course course : rows) {
        var row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(course.getId());
        row.createCell(1).setCellValue(course.getCourseCode());
        row.createCell(2).setCellValue(course.getCourseName());
        row.createCell(3).setCellValue(course.getPrice().doubleValue());
        row.createCell(4).setCellValue(Objects.toString(course.getStartDate(), ""));
        row.createCell(5).setCellValue(Objects.toString(course.getEndDate(), ""));
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new BusinessException(ErrorCode.EXCEL_EXPORT_FAILED);
    }
  }

  public Course get(Long id) {
    return repo.findByIdAndStatus(id, ACTIVE_STATUS)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
  }

  private CourseResponse response(Course course) {
    return mapper.toResponse(
        course, media.findByObjects(ObjectType.COURSE, List.of(course.getId())));
  }

  /**
   * The duplicate check deliberately only considers active rows. A code belonging to a soft-deleted
   * course can therefore be reused. On update, the current row is excluded so retaining its own
   * code is valid.
   */
  private void validate(CourseRequest request, Long currentId) {
    if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.COURSE_INVALID_PRICE);
    }

    boolean duplicateCode =
        currentId == null
            ? repo.existsByCourseCodeIgnoreCaseAndStatus(request.getCourseCode(), ACTIVE_STATUS)
            : repo.existsByCourseCodeIgnoreCaseAndStatusAndIdNot(
                request.getCourseCode(), ACTIVE_STATUS, currentId);
    if (duplicateCode) {
      throw new DuplicateDataException(ErrorCode.COURSE_DUPLICATE_CODE);
    }

    if (request.getStartDate() != null
        && request.getEndDate() != null
        && request.getEndDate().isBefore(request.getStartDate())) {
      throw new BusinessException(ErrorCode.COURSE_INVALID_DATE_RANGE);
    }
  }

  private void normalize(CourseRequest request) {
    if (request.getCourseCode() != null) {
      request.setCourseCode(request.getCourseCode().trim());
    }
    if (request.getCourseName() != null) {
      request.setCourseName(request.getCourseName().trim());
    }
  }

  private String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
