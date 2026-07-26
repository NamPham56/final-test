package com.example.LMS_BE.enrollment.service;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

  private static final int ACTIVE_STATUS = 1;
  private static final BigDecimal COMPLETED_PROGRESS = BigDecimal.valueOf(100);

  private final EnrollmentRepository repo;
  private final StudentRepository students;
  private final CourseRepository courses;
  private final EnrollmentMapper mapper;

  public PageResponse<EnrollmentResponse> search(
      String keyword, EnrollmentStatus status, Long courseId, int page, int size) {
    Page<Enrollment> result =
        repo.search(
            normalizeKeyword(keyword),
            status,
            courseId,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));
    return PageResponse.from(result.map(mapper::toResponse));
  }

  public EnrollmentResponse detail(Long id) {
    Enrollment enrollment =
        repo.findActiveDetailById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND));
    return mapper.toResponse(enrollment);
  }

  @Transactional
  public List<EnrollmentResponse> enroll(EnrollmentRequest request) {
    Student student =
        students
            .findByIdAndStatus(request.getStudentId(), ACTIVE_STATUS)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.STUDENT_NOT_FOUND));

    Long duplicatedRequestCourseId = findFirstDuplicate(request.getCourseIds());
    Set<Long> requestedCourseIds = new LinkedHashSet<>(request.getCourseIds());
    Map<Long, Course> activeCourses = loadActiveCourses(requestedCourseIds);

    Long unavailableCourseId =
        requestedCourseIds.stream()
            .filter(courseId -> !activeCourses.containsKey(courseId))
            .findFirst()
            .orElse(null);
    if (unavailableCourseId != null) {
      throw new ResourceNotFoundException(
          ErrorCode.ENROLLMENT_COURSE_UNAVAILABLE_WITH_ID, unavailableCourseId);
    }

    if (duplicatedRequestCourseId != null) {
      throw new DuplicateDataException(
          ErrorCode.ENROLLMENT_DUPLICATE_COURSE_REQUEST,
          activeCourses.get(duplicatedRequestCourseId).getCourseCode());
    }

    // One query for the whole batch: no repository call inside the course loop.
    List<Enrollment> previousEnrollments =
        repo.findAllForEnrollment(student.getId(), requestedCourseIds);
    Map<Long, List<Enrollment>> previousByCourse =
        previousEnrollments.stream()
            .collect(
                Collectors.groupingBy(
                    enrollment -> enrollment.getCourse().getId(),
                    LinkedHashMap::new,
                    Collectors.toList()));

    for (Long courseId : requestedCourseIds) {
      boolean alreadyActive =
          previousByCourse.getOrDefault(courseId, List.of()).stream()
              .anyMatch(Enrollment::isActive);
      if (alreadyActive) {
        throw new DuplicateDataException(
            ErrorCode.ENROLLMENT_DUPLICATE, activeCourses.get(courseId).getCourseCode());
      }
    }

    List<Enrollment> enrollments = new ArrayList<>(requestedCourseIds.size());
    LocalDateTime enrolledAt = LocalDateTime.now();
    for (Long courseId : requestedCourseIds) {
      Enrollment enrollment = reusableEnrollment(previousByCourse.get(courseId));
      if (enrollment == null) {
        enrollment = mapper.toEntity(request);
      } else {
        enrollment.restore();
      }
      initializeEnrollment(enrollment, student, activeCourses.get(courseId), enrolledAt);
      enrollments.add(enrollment);
    }

    return repo.saveAll(enrollments).stream().map(mapper::toResponse).toList();
  }

  @Transactional
  public EnrollmentResponse update(Long id, EnrollmentUpdateRequest request) {
    Enrollment enrollment = get(id);
    validateProgress(request);
    mapper.updateEntity(request, enrollment);
    applyStatusRules(enrollment);
    return mapper.toResponse(repo.save(enrollment));
  }

  @Transactional
  public void delete(Long id) {
    Enrollment enrollment = get(id);
    enrollment.softDelete();
    repo.save(enrollment);
  }

  public List<EnrollmentResponse> students(Long courseId) {
    courses
        .findByIdAndStatus(courseId, ACTIVE_STATUS)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.COURSE_NOT_FOUND));
    return repo.findActiveStudents(courseId).stream().map(mapper::toResponse).toList();
  }

  private Enrollment get(Long id) {
    return repo.findByIdAndStatus(id, ACTIVE_STATUS)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ENROLLMENT_NOT_FOUND));
  }

  private void validateProgress(EnrollmentUpdateRequest request) {
    if (request.getProgressPercent() == null) {
      return;
    }
    if (request.getProgressPercent().compareTo(BigDecimal.ZERO) < 0
        || request.getProgressPercent().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BusinessException(ErrorCode.ENROLLMENT_INVALID_PROGRESS);
    }
  }

  /**
   * Keeps the persisted progress consistent with the enrollment status. A completed enrollment is
   * always represented by 100% progress, regardless of the progress value submitted by the client.
   */
  private void applyStatusRules(Enrollment enrollment) {
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.COMPLETED) {
      enrollment.setProgressPercent(COMPLETED_PROGRESS);
      if (enrollment.getCompletedAt() == null) {
        enrollment.setCompletedAt(LocalDateTime.now());
      }
      return;
    }
    enrollment.setCompletedAt(null);
  }

  private Map<Long, Course> loadActiveCourses(Collection<Long> ids) {
    return courses.findAllByIdInAndStatus(ids, ACTIVE_STATUS).stream()
        .collect(Collectors.toMap(Course::getId, Function.identity()));
  }

  private Long findFirstDuplicate(List<Long> courseIds) {
    Set<Long> uniqueIds = new LinkedHashSet<>();
    for (Long courseId : courseIds) {
      if (!uniqueIds.add(courseId)) {
        return courseId;
      }
    }
    return null;
  }

  private Enrollment reusableEnrollment(List<Enrollment> previous) {
    if (previous == null) {
      return null;
    }
    // Query order guarantees the latest deleted row is selected if historical
    // duplicate rows exist in a legacy database.
    return previous.stream().filter(Enrollment::isDeleted).findFirst().orElse(null);
  }

  private String normalizeKeyword(String keyword) {
    return keyword == null || keyword.isBlank() ? null : keyword.trim();
  }

  private void initializeEnrollment(
      Enrollment enrollment, Student student, Course course, LocalDateTime enrolledAt) {
    enrollment.setStudent(student);
    enrollment.setCourse(course);
    enrollment.setEnrolledAt(enrolledAt);
    enrollment.setEnrollmentStatus(EnrollmentStatus.ENROLLED);
    enrollment.setProgressPercent(BigDecimal.ZERO);
    enrollment.setCompletedAt(null);
  }
}
