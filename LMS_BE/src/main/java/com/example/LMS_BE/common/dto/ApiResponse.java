package com.example.LMS_BE.common.dto;

import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private boolean success;
  private String code;
  private String message;
  private T data;
  private LocalDateTime timestamp;

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, "SUCCESS", message, data, LocalDateTime.now());
  }

  public static ApiResponse<Void> error(String code, String message) {
    return new ApiResponse<>(false, code, message, null, LocalDateTime.now());
  }
}
