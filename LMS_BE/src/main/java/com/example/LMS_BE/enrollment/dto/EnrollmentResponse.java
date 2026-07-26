package com.example.LMS_BE.enrollment.dto;

import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
  private Long id;
  private Long studentId;
  private String studentCode;
  private String studentName;
  private Long courseId;
  private String courseCode;
  private String courseName;
  private LocalDateTime enrolledAt;
  private EnrollmentStatus enrollmentStatus;
  private BigDecimal progressPercent;
  private LocalDateTime completedAt;
}
