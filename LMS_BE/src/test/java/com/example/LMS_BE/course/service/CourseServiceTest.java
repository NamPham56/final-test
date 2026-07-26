package com.example.LMS_BE.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.course.dto.CourseRequest;
import com.example.LMS_BE.course.dto.CourseResponse;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.course.mapper.CourseMapper;
import com.example.LMS_BE.course.repository.CourseRepository;
import com.example.LMS_BE.enrollment.repository.EnrollmentRepository;
import com.example.LMS_BE.media.service.MediaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private CourseRepository repo;
  @Mock private CourseMapper mapper;
  @Mock private EnrollmentRepository enrollmentRepo;
  @Mock private MediaService media;
  @InjectMocks private CourseService service;

  @Test
  void detailReturnsResponseDtoWithMedia() {
    Course course = new Course();
    course.setId(9L);
    course.setStatus(1);
    CourseResponse response = new CourseResponse();
    response.setId(9L);
    when(repo.findByIdAndStatus(9L, 1)).thenReturn(Optional.of(course));
    when(media.findByObjects(any(), eq(List.of(9L)))).thenReturn(Map.of());
    when(mapper.toResponse(eq(course), any(Map.class))).thenReturn(response);

    assertThat(service.detail(9L)).isSameAs(response);
  }

  @Test
  void create_trimsCodeBeforeCheckingActiveDuplicate() {
    CourseRequest request = request("  CRS-01  ");
    when(repo.existsByCourseCodeIgnoreCaseAndStatus("CRS-01", 1)).thenReturn(true);

    assertThatThrownBy(() -> service.create(request, null, List.of(), List.of(), List.of()))
        .isInstanceOf(DuplicateDataException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COURSE_DUPLICATE_CODE);

    verify(repo).existsByCourseCodeIgnoreCaseAndStatus("CRS-01", 1);
    verify(repo, never()).save(any());
  }

  @Test
  void update_excludesCurrentCourseFromDuplicateCheck() {
    CourseRequest request = request(" CRS-01 ");
    Course course = new Course();
    course.setId(9L);
    course.setStatus(1);
    when(repo.findByIdAndStatus(9L, 1)).thenReturn(Optional.of(course));
    when(mapper.toResponse(eq(course), any(Map.class))).thenReturn(new CourseResponse());
    when(media.findByObjects(any(), any())).thenReturn(Map.of());

    service.update(9L, request, null, List.of(), List.of(), List.of());

    verify(repo).existsByCourseCodeIgnoreCaseAndStatusAndIdNot("CRS-01", 1, 9L);
    verify(repo).save(course);
  }

  @Test
  void create_rejectsEndDateBeforeStartDateWithSpecificError() {
    CourseRequest request = request("CRS-01");
    request.setStartDate(LocalDate.of(2026, 7, 20));
    request.setEndDate(LocalDate.of(2026, 7, 19));

    assertThatThrownBy(() -> service.create(request, null, List.of(), List.of(), List.of()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COURSE_INVALID_DATE_RANGE);

    verify(repo, never()).save(any());
  }

  @Test
  void create_rejectsNegativePriceWithSpecificBusinessError() {
    CourseRequest request = request("CRS-01");
    request.setPrice(BigDecimal.valueOf(-1));

    assertThatThrownBy(() -> service.create(request, null, List.of(), List.of(), List.of()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COURSE_INVALID_PRICE);

    verify(repo, never()).existsByCourseCodeIgnoreCaseAndStatus(any(), anyInt());
    verify(repo, never()).save(any());
  }

  @Test
  void delete_rejectsCourseThatStillHasActiveStudentEnrollments() {
    Course course = new Course();
    course.setId(9L);
    course.setStatus(1);
    when(repo.findByIdAndStatus(9L, 1)).thenReturn(Optional.of(course));
    when(enrollmentRepo.existsByCourseIdAndStatus(9L, 1)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(9L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COURSE_HAS_ACTIVE_ENROLLMENTS);

    verify(repo, never()).save(course);
  }

  @Test
  void delete_softDeletesCourseWithoutActiveStudentEnrollments() {
    Course course = new Course();
    course.setId(9L);
    course.setStatus(1);
    when(repo.findByIdAndStatus(9L, 1)).thenReturn(Optional.of(course));
    when(enrollmentRepo.existsByCourseIdAndStatus(9L, 1)).thenReturn(false);

    service.delete(9L);

    verify(repo).save(course);
    org.assertj.core.api.Assertions.assertThat(course.isDeleted()).isTrue();
  }

  private CourseRequest request(String code) {
    CourseRequest request = new CourseRequest();
    request.setCourseCode(code);
    request.setCourseName("Khóa học Java");
    request.setPrice(BigDecimal.ZERO);
    return request;
  }
}
