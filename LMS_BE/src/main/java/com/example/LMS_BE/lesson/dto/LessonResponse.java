package com.example.LMS_BE.lesson.dto;

import com.example.LMS_BE.media.dto.MediaResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
  private Long id;
  private Long courseId;
  private String lessonCode;
  private String title;
  private String description;
  private Integer durationSeconds;
  private Integer lessonOrder;
  private List<MediaResponse> media;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
