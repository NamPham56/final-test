package com.example.LMS_BE.media.dto;

import com.example.LMS_BE.media.constant.MediaType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
  private Long mediaId;
  private String originalName;
  private String mimeType;
  private Long fileSize;
  private MediaType mediaType;
}
