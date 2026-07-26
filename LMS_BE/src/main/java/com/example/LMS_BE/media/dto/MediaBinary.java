package com.example.LMS_BE.media.dto;

import lombok.*;
import org.springframework.core.io.Resource;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaBinary {
  private Resource resource;
  private String originalName;
  private String mimeType;
  private long fileSize;

  public Resource resource() {
    return resource;
  }

  public String originalName() {
    return originalName;
  }

  public String mimeType() {
    return mimeType;
  }

  public long fileSize() {
    return fileSize;
  }
}
