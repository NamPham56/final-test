package com.example.LMS_BE.lesson.repository;

import com.example.LMS_BE.lesson.entity.Lesson;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
  Optional<Lesson> findByIdAndStatus(Long id, Integer status);

  @Query(
      """
      select l
      from Lesson l
      join fetch l.course c
      where l.id = :id
        and l.status = 1
        and c.status = 1
      """)
  Optional<Lesson> findActiveDetailById(@Param("id") Long id);

  @Query(
      """
      select l
      from Lesson l
      join fetch l.course c
      where c.id = :courseId
        and l.status = 1
        and c.status = 1
      order by l.lessonOrder
      """)
  List<Lesson> findActiveByCourse(@Param("courseId") Long courseId);

  boolean existsByCourseIdAndLessonCodeIgnoreCaseAndStatus(
      Long courseId, String code, Integer status);

  boolean existsByCourseIdAndLessonCodeIgnoreCaseAndStatusAndIdNot(
      Long courseId, String code, Integer status, Long id);
}
