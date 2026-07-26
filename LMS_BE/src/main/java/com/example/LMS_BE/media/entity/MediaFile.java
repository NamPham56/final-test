package com.example.LMS_BE.media.entity;

import com.example.LMS_BE.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media_files")
public class MediaFile extends BaseEntity {

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 255)
  private String storedName;

  @Column(name = "file_path", nullable = false, length = 500)
  private String filePath;

  @Column(name = "file_url", length = 500)
  private String fileUrl;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @Column(name = "extension", length = 20)
  private String extension;

  @Column(name = "file_size")
  private Long fileSize;
}
