package com.example.LMS_BE.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequest {
  @NotNull @Positive private Long courseId;

  @NotBlank
  @Size(max = 50)
  private String lessonCode;

  @NotBlank
  @Size(max = 200)
  private String title;

  private String description;

  @PositiveOrZero private Integer durationSeconds;

  @NotNull private Integer lessonOrder;

  private List<@NotNull @Positive Long> retainedMediaIds;
  private List<@NotNull @Positive Long> newMediaIds;
  private List<@NotNull @Positive Long> removedMediaIds;

  public Long courseId() {
    return courseId;
  }

  public String lessonCode() {
    return lessonCode;
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
