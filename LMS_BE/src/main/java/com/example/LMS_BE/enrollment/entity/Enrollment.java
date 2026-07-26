package com.example.LMS_BE.enrollment.entity;

import com.example.LMS_BE.common.BaseEntity;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import com.example.LMS_BE.student.enity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "enrollments")
public class Enrollment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private Course course;

  @Column(name = "enrolled_at", nullable = false)
  private LocalDateTime enrolledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "enrollment_status", nullable = false, length = 30)
  private EnrollmentStatus enrollmentStatus;

  @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
  private BigDecimal progressPercent;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;
}
