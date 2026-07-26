package com.example.LMS_BE.course.dto;

import com.example.LMS_BE.media.dto.MediaResponse;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
  private Long id;
  private String courseCode;
  private String courseName;
  private String description;
  private BigDecimal price;
  private LocalDate startDate;
  private LocalDate endDate;
  private List<MediaResponse> media;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
