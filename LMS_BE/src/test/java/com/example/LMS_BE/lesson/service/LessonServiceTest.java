package com.example.LMS_BE.lesson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.course.repository.CourseRepository;
import com.example.LMS_BE.lesson.dto.LessonRequest;
import com.example.LMS_BE.lesson.dto.LessonResponse;
import com.example.LMS_BE.lesson.entity.Lesson;
import com.example.LMS_BE.lesson.mapper.LessonMapper;
import com.example.LMS_BE.lesson.repository.LessonRepository;
import com.example.LMS_BE.media.service.MediaService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

  @Mock private LessonRepository repo;
  @Mock private CourseRepository courses;
  @Mock private LessonMapper mapper;
  @Mock private MediaService media;
  @InjectMocks private LessonService service;

  @Test
  void detailUsesJoinedCourseQueryAndReturnsResponseDto() {
    Course course = course(2L);
    Lesson lesson = new Lesson();
    lesson.setId(8L);
    lesson.setStatus(1);
    lesson.setCourse(course);
    LessonResponse response = new LessonResponse();
    response.setId(8L);
    when(repo.findActiveDetailById(8L)).thenReturn(Optional.of(lesson));
    when(media.findByObjects(any(), eq(List.of(8L)))).thenReturn(Map.of());
    when(mapper.toResponse(eq(lesson), any())).thenReturn(response);

    assertThat(service.detail(8L)).isSameAs(response);
  }

  @Test
  void create_trimsCodeAndChecksDuplicateOnlyInActiveCourseLessons() {
    LessonRequest request = request("  LES-01  ");
    Course course = course(2L);
    when(courses.findByIdAndStatus(2L, 1)).thenReturn(Optional.of(course));
    when(repo.existsByCourseIdAndLessonCodeIgnoreCaseAndStatus(2L, "LES-01", 1)).thenReturn(true);

    assertThatThrownBy(() -> service.create(request, null, List.of(), null, List.of(), List.of()))
        .isInstanceOf(DuplicateDataException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LESSON_DUPLICATE_CODE);

    verify(repo).existsByCourseIdAndLessonCodeIgnoreCaseAndStatus(2L, "LES-01", 1);
    verify(repo, never()).save(any());
  }

  @Test
  void update_excludesCurrentLessonWhenCheckingCode() {
    LessonRequest request = request(" LES-01 ");
    Course course = course(2L);
    Lesson lesson = new Lesson();
    lesson.setId(8L);
    lesson.setStatus(1);
    lesson.setCourse(course);
    when(repo.findByIdAndStatus(8L, 1)).thenReturn(Optional.of(lesson));
    when(courses.findByIdAndStatus(2L, 1)).thenReturn(Optional.of(course));

    service.update(8L, request, null, List.of(), null, List.of(), List.of());

    verify(repo).existsByCourseIdAndLessonCodeIgnoreCaseAndStatusAndIdNot(2L, "LES-01", 1, 8L);
    verify(repo).save(lesson);
  }

  @Test
  void create_rejectsLessonOrderLessThanOneBeforeDatabaseSave() {
    LessonRequest request = request("LES-01");
    request.setLessonOrder(0);

    assertThatThrownBy(() -> service.create(request, null, List.of(), null, List.of(), List.of()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LESSON_INVALID_ORDER);

    verify(courses, never()).findByIdAndStatus(any(), any());
    verify(repo, never()).save(any());
  }

  @Test
  void create_rejectsNegativeDurationBeforeDatabaseSave() {
    LessonRequest request = request("LES-01");
    request.setDurationSeconds(-1);

    assertThatThrownBy(() -> service.create(request, null, List.of(), null, List.of(), List.of()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LESSON_INVALID_DURATION);

    verify(courses, never()).findByIdAndStatus(any(), any());
    verify(repo, never()).save(any());
  }

  private LessonRequest request(String code) {
    LessonRequest request = new LessonRequest();
    request.setCourseId(2L);
    request.setLessonCode(code);
    request.setTitle("Bài mở đầu");
    request.setLessonOrder(1);
    return request;
  }

  private Course course(Long id) {
    Course course = new Course();
    course.setId(id);
    course.setStatus(1);
    course.setCourseCode("CRS-01");
    return course;
  }
}
