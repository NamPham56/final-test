package com.example.LMS_BE.student.dto;

import com.example.LMS_BE.media.dto.MediaResponse;
import java.time.*;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
  private Long id;
  private String studentCode;
  private String fullName;
  private String email;
  private String phone;
  private LocalDate dateOfBirth;
  private String gender;
  private String address;
  private List<MediaResponse> media;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
