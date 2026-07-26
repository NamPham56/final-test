package com.example.LMS_BE.enrollment.repository;

import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import com.example.LMS_BE.enrollment.entity.Enrollment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  Optional<Enrollment> findByIdAndStatus(Long id, Integer status);

  @Query(
      """
            select e
            from Enrollment e
            join fetch e.student s
            join fetch e.course c
            where e.id = :id
              and e.status = 1
              and s.status = 1
              and c.status = 1
            """)
  Optional<Enrollment> findActiveDetailById(@Param("id") Long id);

  @Query(
      value =
          """
                    select e
                    from Enrollment e
                    join fetch e.student s
                    join fetch e.course c
                    where e.status = 1
                      and s.status = 1
                      and c.status = 1
                      and (:courseId is null or c.id = :courseId)
                      and (:enrollmentStatus is null or e.enrollmentStatus = :enrollmentStatus)
                      and (
                          :keyword is null
                          or lower(s.studentCode) like lower(concat('%', :keyword, '%'))
                          or lower(s.fullName) like lower(concat('%', :keyword, '%'))
                          or lower(s.email) like lower(concat('%', :keyword, '%'))
                          or lower(c.courseCode) like lower(concat('%', :keyword, '%'))
                          or lower(c.courseName) like lower(concat('%', :keyword, '%'))
                      )
                    """,
      countQuery =
          """
                    select count(e)
                    from Enrollment e
                    join e.student s
                    join e.course c
                    where e.status = 1
                      and s.status = 1
                      and c.status = 1
                      and (:courseId is null or c.id = :courseId)
                      and (:enrollmentStatus is null or e.enrollmentStatus = :enrollmentStatus)
                      and (
                          :keyword is null
                          or lower(s.studentCode) like lower(concat('%', :keyword, '%'))
                          or lower(s.fullName) like lower(concat('%', :keyword, '%'))
                          or lower(s.email) like lower(concat('%', :keyword, '%'))
                          or lower(c.courseCode) like lower(concat('%', :keyword, '%'))
                          or lower(c.courseName) like lower(concat('%', :keyword, '%'))
                      )
                    """)
  Page<Enrollment> search(
      @Param("keyword") String keyword,
      @Param("enrollmentStatus") EnrollmentStatus enrollmentStatus,
      @Param("courseId") Long courseId,
      Pageable pageable);

  /**
   * Used before soft-deleting a course. This is an existence query, therefore it does not load
   * enrollments or trigger an N+1 query.
   */
  boolean existsByCourseIdAndStatus(Long courseId, Integer status);

  /**
   * Loads every previous enrollment for the requested course set in one query. Deleted rows are
   * included so the service can restore them instead of creating another physical row. Both
   * relations are fetched for DTO mapping without N+1.
   */
  @Query(
      """
            select e
            from Enrollment e
            join fetch e.student s
            join fetch e.course c
            where s.id = :studentId
              and c.id in :courseIds
            order by e.status desc, e.id desc
            """)
  List<Enrollment> findAllForEnrollment(
      @Param("studentId") Long studentId, @Param("courseIds") Collection<Long> courseIds);

  @Query(
      """
            select e
            from Enrollment e
            join fetch e.student s
            join fetch e.course c
            where c.id = :courseId
              and e.status = 1
              and s.status = 1
              and c.status = 1
            order by s.fullName
            """)
  List<Enrollment> findActiveStudents(@Param("courseId") Long courseId);
}
