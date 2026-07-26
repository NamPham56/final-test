package com.example.LMS_BE.course.repository;

import com.example.LMS_BE.course.entity.Course;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {
  Optional<Course> findByIdAndStatus(Long id, Integer status);

  boolean existsByCourseCodeIgnoreCaseAndStatus(String code, Integer status);

  boolean existsByCourseCodeIgnoreCaseAndStatusAndIdNot(String code, Integer status, Long id);

  @Query(
      """
      select c
      from Course c
      where c.status = 1
        and (
          :keyword is null
          or lower(c.courseCode) like lower(concat('%', :keyword, '%'))
          or lower(c.courseName) like lower(concat('%', :keyword, '%'))
        )
        and (:fromDate is null or c.startDate >= :fromDate)
        and (:toDate is null or c.endDate <= :toDate)
      """)
  Page<Course> search(
      @Param("keyword") String keyword,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate,
      Pageable pageable);

  @Query(
      """
      select c
      from Course c
      where c.status = 1
        and (
          :keyword is null
          or lower(c.courseCode) like lower(concat('%', :keyword, '%'))
          or lower(c.courseName) like lower(concat('%', :keyword, '%'))
        )
        and (:fromDate is null or c.startDate >= :fromDate)
        and (:toDate is null or c.endDate <= :toDate)
      order by c.createdAt desc
      """)
  List<Course> export(
      @Param("keyword") String keyword,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);

  List<Course> findAllByIdInAndStatus(Collection<Long> ids, Integer status);
}
