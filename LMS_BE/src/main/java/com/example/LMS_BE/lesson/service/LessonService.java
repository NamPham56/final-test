package com.example.LMS_BE.lesson.service;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.DuplicateDataException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.common.exception.ResourceNotFoundException;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.course.repository.CourseRepository;
import com.example.LMS_BE.lesson.dto.LessonRequest;
import com.example.LMS_BE.lesson.dto.LessonResponse;
import com.example.LMS_BE.lesson.entity.Lesson;
import com.example.LMS_BE.lesson.mapper.LessonMapper;
import com.example.LMS_BE.lesson.repository.LessonRepository;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.service.MediaService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LessonService {

  private static final int ACTIVE_STATUS = 1;

  private final LessonRepository repo;
  private final CourseRepository courses;
  private final LessonMapper mapper;
  private final MediaService media;

  public List<LessonResponse> list(Long courseId) {
    getActiveCourse(courseId);
    List<Lesson> lessons = repo.findActiveByCourse(courseId);
    Map<Long, List<MediaResponse>> mediaMap =
        media.findByObjects(ObjectType.LESSON, lessons.stream().map(Lesson::getId).toList());
    return lessons.stream().map(lesson -> mapper.toResponse(lesson, mediaMap)).toList();
  }

  public LessonResponse detail(Long id) {
    Lesson lesson =
        repo.findActiveDetailById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.LESSON_NOT_FOUND));
    return response(lesson);
  }

  @Transactional
  public LessonResponse create(
      LessonRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> thumbnails,
      MultipartFile video,
      List<MultipartFile> videos,
      List<MultipartFile> images) {
    normalize(request);
    validateBusinessRules(request);
    Course course = getActiveCourse(request.getCourseId());
    validateCode(request, null);

    Lesson lesson = mapper.toEntity(request);
    lesson.setCourse(course);
    repo.save(lesson);
    media.sync(
        ObjectType.LESSON,
        lesson.getId(),
        List.of(),
        request.getNewMediaIds(),
        List.of(),
        MediaType.IMAGE);
    media.attach(ObjectType.LESSON, lesson.getId(), media.store(thumbnail), MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.LESSON, lesson.getId(), thumbnails, MediaType.THUMBNAIL);
    media.attach(ObjectType.LESSON, lesson.getId(), media.store(video), MediaType.VIDEO);
    media.attachUploaded(ObjectType.LESSON, lesson.getId(), videos, MediaType.VIDEO);
    media.attachUploaded(ObjectType.LESSON, lesson.getId(), images, MediaType.IMAGE);
    return response(lesson);
  }

  @Transactional
  public LessonResponse update(
      Long id,
      LessonRequest request,
      MultipartFile thumbnail,
      List<MultipartFile> thumbnails,
      MultipartFile video,
      List<MultipartFile> videos,
      List<MultipartFile> images) {
    Lesson lesson = get(id);
    normalize(request);
    validateBusinessRules(request);
    Course course = getActiveCourse(request.getCourseId());
    validateCode(request, id);

    mapper.updateEntity(request, lesson);
    lesson.setCourse(course);
    repo.save(lesson);
    media.sync(
        ObjectType.LESSON,
        id,
        request.getRetainedMediaIds(),
        request.getNewMediaIds(),
        request.getRemovedMediaIds(),
        MediaType.IMAGE);
    media.attach(ObjectType.LESSON, id, media.store(thumbnail), MediaType.THUMBNAIL);
    media.attachUploaded(ObjectType.LESSON, id, thumbnails, MediaType.THUMBNAIL);
    media.attach(ObjectType.LESSON, id, media.store(video), MediaType.VIDEO);
    media.attachUploaded(ObjectType.LESSON, id, videos, MediaType.VIDEO);
    media.attachUploaded(ObjectType.LESSON, id, images, MediaType.IMAGE);
    return response(lesson);
  }

  @Transactional
  public void delete(Long id) {
    Lesson lesson = get(id);
    lesson.softDelete();
    repo.save(lesson);
  }

  private LessonResponse response(Lesson lesson) {
    return mapper.toResponse(
        lesson, media.findByObjects(ObjectType.LESSON, List.of(lesson.getId())));
  }

  private Lesson get(Long id) {
    return repo.findByIdAndStatus(id, ACTIVE_STATUS)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.LESSON_NOT_FOUND));
  }

  private Course getActiveCourse(Long id) {
    return courses
        .findByIdAndStatus(id, ACTIVE_STATUS)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
  }

  /**
   * Lesson codes are unique only among active lessons in the same course. The current lesson is
   * excluded during update, while codes on soft-deleted rows remain reusable.
   */
  private void validateCode(LessonRequest request, Long currentId) {
    boolean duplicateCode =
        currentId == null
            ? repo.existsByCourseIdAndLessonCodeIgnoreCaseAndStatus(
                request.getCourseId(), request.getLessonCode(), ACTIVE_STATUS)
            : repo.existsByCourseIdAndLessonCodeIgnoreCaseAndStatusAndIdNot(
                request.getCourseId(), request.getLessonCode(), ACTIVE_STATUS, currentId);
    if (duplicateCode) {
      throw new DuplicateDataException(ErrorCode.LESSON_DUPLICATE_CODE);
    }
  }

  private void validateBusinessRules(LessonRequest request) {
    if (request.getLessonOrder() != null && request.getLessonOrder() < 1) {
      throw new BusinessException(ErrorCode.LESSON_INVALID_ORDER);
    }
    if (request.getDurationSeconds() != null && request.getDurationSeconds() < 0) {
      throw new BusinessException(ErrorCode.LESSON_INVALID_DURATION);
    }
  }

  private void normalize(LessonRequest request) {
    if (request.getLessonCode() != null) {
      request.setLessonCode(request.getLessonCode().trim());
    }
    if (request.getTitle() != null) {
      request.setTitle(request.getTitle().trim());
    }
  }
}
