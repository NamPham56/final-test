package com.example.LMS_BE.student.repository;

import com.example.LMS_BE.student.enity.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

  Optional<Student> findByIdAndStatus(Long id, Integer status);

  boolean existsByStudentCodeIgnoreCaseAndStatus(String code, Integer status);

  boolean existsByStudentCodeIgnoreCaseAndStatusAndIdNot(String code, Integer status, Long id);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  @Query(
      """
      select s
      from Student s
      where s.status = 1
        and (
          :keyword is null
          or lower(s.studentCode) like lower(concat('%', :keyword, '%'))
          or lower(s.fullName) like lower(concat('%', :keyword, '%'))
          or lower(s.email) like lower(concat('%', :keyword, '%'))
          or lower(coalesce(s.phone, '')) like lower(concat('%', :keyword, '%'))
        )
      """)
  Page<Student> search(@Param("keyword") String keyword, Pageable pageable);

  @Query(
      """
      select s
      from Student s
      where s.status = 1
        and (
          :keyword is null
          or lower(s.studentCode) like lower(concat('%', :keyword, '%'))
          or lower(s.fullName) like lower(concat('%', :keyword, '%'))
          or lower(s.email) like lower(concat('%', :keyword, '%'))
          or lower(coalesce(s.phone, '')) like lower(concat('%', :keyword, '%'))
        )
      order by s.createdAt desc
      """)
  List<Student> export(@Param("keyword") String keyword);
}
