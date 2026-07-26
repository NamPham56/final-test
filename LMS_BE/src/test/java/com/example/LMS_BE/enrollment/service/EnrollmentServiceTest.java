package com.example.LMS_BE.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.common.exception.ResourceNotFoundException;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.course.repository.CourseRepository;
import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import com.example.LMS_BE.enrollment.dto.EnrollmentRequest;
import com.example.LMS_BE.enrollment.dto.EnrollmentResponse;
import com.example.LMS_BE.enrollment.dto.EnrollmentUpdateRequest;
import com.example.LMS_BE.enrollment.entity.Enrollment;
import com.example.LMS_BE.enrollment.mapper.EnrollmentMapper;
import com.example.LMS_BE.enrollment.repository.EnrollmentRepository;
import com.example.LMS_BE.student.enity.Student;
import com.example.LMS_BE.student.repository.StudentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

  @Mock private EnrollmentRepository repo;
  @Mock private StudentRepository students;
  @Mock private CourseRepository courses;
  @Mock private EnrollmentMapper mapper;
  @InjectMocks private EnrollmentService service;

  @Test
  void searchReturnsCustomPageAndMapsAlreadyJoinedRelations() {
    Student student = student();
    Course course = course(5L, "CRS-05");
    Enrollment enrollment = enrollment(student, course, 1);
    EnrollmentResponse response = new EnrollmentResponse();
    response.setId(20L);
    PageRequest pageable = PageRequest.of(0, 20);

    when(repo.search(eq("Nam"), eq(EnrollmentStatus.LEARNING), eq(5L), any()))
        .thenReturn(new PageImpl<>(List.of(enrollment), pageable, 1));
    when(mapper.toResponse(enrollment)).thenReturn(response);

    PageResponse<EnrollmentResponse> result =
        service.search("  Nam  ", EnrollmentStatus.LEARNING, 5L, 0, 20);

    assertThat(result.getItems()).containsExactly(response);
    assertThat(result.getPage()).isZero();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotalItems()).isEqualTo(1);
    assertThat(result.isHasNext()).isFalse();
  }

  @Test
  void detailReturnsMappedDtoFromJoinedDetailQuery() {
    Enrollment enrollment = enrollment(student(), course(5L, "CRS-05"), 1);
    EnrollmentResponse response = new EnrollmentResponse();
    response.setId(20L);
    when(repo.findActiveDetailById(20L)).thenReturn(Optional.of(enrollment));
    when(mapper.toResponse(enrollment)).thenReturn(response);

    assertThat(service.detail(20L)).isSameAs(response);
    verify(repo).findActiveDetailById(20L);
  }

  @Test
  void enroll_rejectsRepeatedCourseInSameRequestWithSpecificError() {
    EnrollmentRequest request = request(List.of(5L, 5L));
    Student student = student();
    Course course = course(5L, "CRS-05");
    when(students.findByIdAndStatus(1L, 1)).thenReturn(Optional.of(student));
    when(courses.findAllByIdInAndStatus(anyCollection(), any())).thenReturn(List.of(course));

    assertThatThrownBy(() -> service.enroll(request))
        .isInstanceOf(DuplicateDataException.class)
        .satisfies(
            exception -> {
              DuplicateDataException duplicate = (DuplicateDataException) exception;
              assertThat(duplicate.getErrorCode())
                  .isEqualTo(ErrorCode.ENROLLMENT_DUPLICATE_COURSE_REQUEST);
              assertThat(duplicate.getArguments()).containsExactly("CRS-05");
            });

    verify(repo, never()).findAllForEnrollment(any(), anyCollection());
    verify(repo, never()).saveAll(any());
  }

  @Test
  void enroll_reportsTheExactUnavailableCourseId() {
    EnrollmentRequest request = request(List.of(5L, 99L));
    when(students.findByIdAndStatus(1L, 1)).thenReturn(Optional.of(student()));
    when(courses.findAllByIdInAndStatus(anyCollection(), any()))
        .thenReturn(List.of(course(5L, "CRS-05")));

    assertThatThrownBy(() -> service.enroll(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .satisfies(
            exception -> {
              ResourceNotFoundException notFound = (ResourceNotFoundException) exception;
              assertThat(notFound.getErrorCode())
                  .isEqualTo(ErrorCode.ENROLLMENT_COURSE_UNAVAILABLE_WITH_ID);
              assertThat(notFound.getArguments()).containsExactly(99L);
            });

    verify(repo, never()).findAllForEnrollment(any(), anyCollection());
  }

  @Test
  void enroll_detectsExistingActiveEnrollmentUsingOneBatchQuery() {
    EnrollmentRequest request = request(List.of(5L));
    Student student = student();
    Course course = course(5L, "CRS-05");
    Enrollment existing = enrollment(student, course, 1);
    when(students.findByIdAndStatus(1L, 1)).thenReturn(Optional.of(student));
    when(courses.findAllByIdInAndStatus(anyCollection(), any())).thenReturn(List.of(course));
    when(repo.findAllForEnrollment(eq(1L), anyCollection())).thenReturn(List.of(existing));

    assertThatThrownBy(() -> service.enroll(request))
        .isInstanceOf(DuplicateDataException.class)
        .satisfies(
            exception -> {
              DuplicateDataException duplicate = (DuplicateDataException) exception;
              assertThat(duplicate.getErrorCode()).isEqualTo(ErrorCode.ENROLLMENT_DUPLICATE);
              assertThat(duplicate.getArguments()).containsExactly("CRS-05");
            });

    verify(repo).findAllForEnrollment(eq(1L), anyCollection());
    verify(repo, never()).saveAll(any());
  }

  @Test
  void enroll_restoresSoftDeletedEnrollmentInsteadOfCreatingAnotherRow() {
    EnrollmentRequest request = request(List.of(5L));
    Student student = student();
    Course course = course(5L, "CRS-05");
    Enrollment deleted = enrollment(student, course, 0);
    deleted.setId(20L);
    when(students.findByIdAndStatus(1L, 1)).thenReturn(Optional.of(student));
    when(courses.findAllByIdInAndStatus(anyCollection(), any())).thenReturn(List.of(course));
    when(repo.findAllForEnrollment(eq(1L), anyCollection())).thenReturn(List.of(deleted));
    when(repo.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.enroll(request);

    assertThat(deleted.isActive()).isTrue();
    verify(mapper, never()).toEntity(request);
    verify(repo).saveAll(List.of(deleted));
  }

  @Test
  void update_rejectsInvalidProgressWithSpecificBusinessError() {
    Enrollment enrollment = enrollment(student(), course(5L, "CRS-05"), 1);
    EnrollmentUpdateRequest request = new EnrollmentUpdateRequest();
    request.setEnrollmentStatus(EnrollmentStatus.ENROLLED);
    request.setProgressPercent(BigDecimal.valueOf(101));
    when(repo.findByIdAndStatus(20L, 1)).thenReturn(Optional.of(enrollment));

    assertThatThrownBy(() -> service.update(20L, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ENROLLMENT_INVALID_PROGRESS);

    verify(mapper, never()).updateEntity(any(), any());
    verify(repo, never()).save(any());
  }

  @Test
  void update_setsProgressToOneHundredWhenStatusIsCompleted() {
    Enrollment enrollment = enrollment(student(), course(5L, "CRS-05"), 1);
    enrollment.setId(20L);
    enrollment.setEnrollmentStatus(EnrollmentStatus.LEARNING);
    enrollment.setProgressPercent(BigDecimal.valueOf(45));

    EnrollmentUpdateRequest request = new EnrollmentUpdateRequest();
    request.setEnrollmentStatus(EnrollmentStatus.COMPLETED);
    request.setProgressPercent(BigDecimal.valueOf(80));

    when(repo.findByIdAndStatus(20L, 1)).thenReturn(Optional.of(enrollment));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              EnrollmentUpdateRequest source = invocation.getArgument(0);
              Enrollment target = invocation.getArgument(1);
              target.setEnrollmentStatus(source.getEnrollmentStatus());
              target.setProgressPercent(source.getProgressPercent());
              return null;
            })
        .when(mapper)
        .updateEntity(request, enrollment);
    when(repo.save(enrollment)).thenReturn(enrollment);

    service.update(20L, request);

    assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    assertThat(enrollment.getProgressPercent()).isEqualByComparingTo("100");
    assertThat(enrollment.getCompletedAt()).isNotNull();
    verify(repo).save(enrollment);
  }

  @Test
  void update_preservesOriginalCompletionTimeWhenAlreadyCompleted() {
    Enrollment enrollment = enrollment(student(), course(5L, "CRS-05"), 1);
    enrollment.setId(20L);
    enrollment.setEnrollmentStatus(EnrollmentStatus.COMPLETED);
    enrollment.setProgressPercent(BigDecimal.valueOf(100));
    LocalDateTime originalCompletedAt = LocalDateTime.of(2026, 7, 20, 9, 30);
    enrollment.setCompletedAt(originalCompletedAt);

    EnrollmentUpdateRequest request = new EnrollmentUpdateRequest();
    request.setEnrollmentStatus(EnrollmentStatus.COMPLETED);
    request.setProgressPercent(BigDecimal.valueOf(60));

    when(repo.findByIdAndStatus(20L, 1)).thenReturn(Optional.of(enrollment));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              EnrollmentUpdateRequest source = invocation.getArgument(0);
              Enrollment target = invocation.getArgument(1);
              target.setEnrollmentStatus(source.getEnrollmentStatus());
              target.setProgressPercent(source.getProgressPercent());
              return null;
            })
        .when(mapper)
        .updateEntity(request, enrollment);
    when(repo.save(enrollment)).thenReturn(enrollment);

    service.update(20L, request);

    assertThat(enrollment.getProgressPercent()).isEqualByComparingTo("100");
    assertThat(enrollment.getCompletedAt()).isEqualTo(originalCompletedAt);
  }

  private EnrollmentRequest request(List<Long> courseIds) {
    EnrollmentRequest request = new EnrollmentRequest();
    request.setStudentId(1L);
    request.setCourseIds(courseIds);
    return request;
  }

  private Student student() {
    Student student = new Student();
    student.setId(1L);
    student.setStatus(1);
    return student;
  }

  private Course course(Long id, String code) {
    Course course = new Course();
    course.setId(id);
    course.setStatus(1);
    course.setCourseCode(code);
    return course;
  }

  private Enrollment enrollment(Student student, Course course, int status) {
    Enrollment enrollment = new Enrollment();
    enrollment.setStudent(student);
    enrollment.setCourse(course);
    enrollment.setStatus(status);
    return enrollment;
  }
}
