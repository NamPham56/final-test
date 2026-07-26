package com.example.LMS_BE.enrollment.dto;

import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentUpdateRequest {
  @NotNull private EnrollmentStatus enrollmentStatus;

  @NotNull
  @DecimalMin("0")
  @DecimalMax("100")
  private BigDecimal progressPercent;

  public EnrollmentStatus enrollmentStatus() {
    return enrollmentStatus;
  }
}
