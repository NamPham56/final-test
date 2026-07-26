package com.example.LMS_BE.student.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.media.service.MediaService;
import com.example.LMS_BE.student.dto.StudentRequest;
import com.example.LMS_BE.student.dto.StudentResponse;
import com.example.LMS_BE.student.enity.Student;
import com.example.LMS_BE.student.mapper.StudentMapper;
import com.example.LMS_BE.student.repository.StudentRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

  @Mock private StudentRepository repository;

  @Mock private StudentMapper mapper;

  @Mock private MediaService mediaService;

  @InjectMocks private StudentService service;

  @Test
  void detailReturnsResponseDtoWithMedia() {
    Student student = new Student();
    student.setId(12L);
    student.setStatus(1);
    StudentResponse response = new StudentResponse();
    response.setId(12L);
    when(repository.findByIdAndStatus(12L, 1)).thenReturn(Optional.of(student));
    when(mediaService.findByObjects(any(), eq(List.of(12L)))).thenReturn(java.util.Map.of());
    when(mapper.toResponse(eq(student), any())).thenReturn(response);

    assertThat(service.detail(12L)).isSameAs(response);
  }

  @Test
  void createRejectsActiveCodeAfterTrimming() {
    StudentRequest request = request("  SV001  ", "Nguyen Van A", "student@example.com");
    when(repository.existsByStudentCodeIgnoreCaseAndStatus("SV001", 1)).thenReturn(true);

    DuplicateDataException exception =
        assertThrows(DuplicateDataException.class, () -> service.create(request, null, List.of()));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STUDENT_DUPLICATE_CODE);
    verify(repository, never()).existsByEmailIgnoreCase("student@example.com");
  }

  @Test
  void createRejectsEmailGloballyWithCaseAndWhitespaceNormalized() {
    StudentRequest request = request("SV001", "Nguyen Van A", "  Student@Example.COM  ");
    when(repository.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);

    DuplicateDataException exception =
        assertThrows(DuplicateDataException.class, () -> service.create(request, null, List.of()));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STUDENT_DUPLICATE_EMAIL);
    verify(repository).existsByStudentCodeIgnoreCaseAndStatus("SV001", 1);
    verify(repository).existsByEmailIgnoreCase("student@example.com");
  }

  @Test
  void updateChecksCodeAmongActiveRowsAndEmailAmongAllRowsExcludingItself() {
    Student existing = new Student();
    existing.setId(9L);
    existing.setStatus(1);
    when(repository.findByIdAndStatus(9L, 1)).thenReturn(Optional.of(existing));

    StudentRequest request = request("  Sv009 ", "Nguyen Van B", " USER@EXAMPLE.COM ");
    when(repository.existsByEmailIgnoreCaseAndIdNot("user@example.com", 9L)).thenReturn(true);

    DuplicateDataException exception =
        assertThrows(
            DuplicateDataException.class, () -> service.update(9L, request, null, List.of()));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STUDENT_DUPLICATE_EMAIL);
    verify(repository).existsByStudentCodeIgnoreCaseAndStatusAndIdNot("Sv009", 1, 9L);
    verify(repository).existsByEmailIgnoreCaseAndIdNot("user@example.com", 9L);
  }

  @Test
  void createStoresCanonicalValuesAndAllowsCodeFromDeletedRow() {
    StudentRequest request = request("  sv-deleted  ", "  Nguyen Van C  ", "  USER@Example.COM  ");
    request.setPhone("  0901234567  ");
    request.setGender("  NAM  ");
    request.setAddress("  Ha Noi  ");

    Student mapped = new Student();
    when(mapper.toEntity(request)).thenReturn(mapped);
    when(repository.saveAndFlush(mapped))
        .thenAnswer(
            invocation -> {
              mapped.setId(12L);
              return mapped;
            });

    service.create(request, null, List.of());

    ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
    verify(repository).saveAndFlush(captor.capture());
    Student saved = captor.getValue();
    assertThat(saved.getStudentCode()).isEqualTo("sv-deleted");
    assertThat(saved.getFullName()).isEqualTo("Nguyen Van C");
    assertThat(saved.getEmail()).isEqualTo("user@example.com");
    assertThat(saved.getPhone()).isEqualTo("0901234567");
    assertThat(saved.getGender()).isEqualTo("NAM");
    assertThat(saved.getAddress()).isEqualTo("Ha Noi");
  }

  @Test
  void createTranslatesDatabaseDuplicateToEmailError() {
    StudentRequest request = request("SV001", "Nguyen Van A", "student@example.com");
    Student mapped = new Student();
    when(mapper.toEntity(request)).thenReturn(mapped);
    SQLException cause =
        new SQLException(
            "Duplicate entry 'student@example.com' for key 'UK_opaque_constraint_name'",
            "23000",
            1062);
    when(repository.saveAndFlush(mapped))
        .thenThrow(new DataIntegrityViolationException("Could not execute statement", cause));

    DuplicateDataException exception =
        assertThrows(DuplicateDataException.class, () -> service.create(request, null, List.of()));

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.STUDENT_DUPLICATE_EMAIL);
  }

  private StudentRequest request(String code, String fullName, String email) {
    StudentRequest request = new StudentRequest();
    request.setStudentCode(code);
    request.setFullName(fullName);
    request.setEmail(email);
    return request;
  }
}
