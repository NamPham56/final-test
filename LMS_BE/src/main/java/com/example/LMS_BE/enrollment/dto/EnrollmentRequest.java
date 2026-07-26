package com.example.LMS_BE.enrollment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentRequest {
  @NotNull private Long studentId;
  @NotEmpty private List<@NotNull Long> courseIds;

  public Long studentId() {
    return studentId;
  }

  public List<Long> courseIds() {
    return courseIds;
  }
}
