package com.example.LMS_BE.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequest {
  @NotBlank
  @Size(max = 50)
  private String courseCode;

  @NotBlank
  @Size(max = 200)
  private String courseName;

  private String description;

  @NotNull @PositiveOrZero private BigDecimal price;

  private LocalDate startDate;
  private LocalDate endDate;

  private List<@NotNull @Positive Long> retainedMediaIds;
  private List<@NotNull @Positive Long> newMediaIds;
  private List<@NotNull @Positive Long> removedMediaIds;

  public String courseCode() {
    return courseCode;
  }

  public LocalDate startDate() {
    return startDate;
  }

  public LocalDate endDate() {
    return endDate;
  }

  public List<Long> retainedMediaIds() {
    return retainedMediaIds;
  }

  public List<Long> newMediaIds() {
    return newMediaIds;
  }

  public List<Long> removedMediaIds() {
    return removedMediaIds;
  }
}
