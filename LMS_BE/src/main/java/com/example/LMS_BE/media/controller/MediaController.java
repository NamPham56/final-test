package com.example.LMS_BE.media.controller;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.dto.MediaBinary;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.service.MediaService;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {
  private final MediaService mediaService;
  private final MessageUtils messages;

  @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<MediaResponse>> upload(
      @RequestPart MultipartFile file, @RequestParam MediaType mediaType) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                messages.get("media.uploaded"), mediaService.upload(file, mediaType)));
  }

  @GetMapping("/{mediaId}/content")
  public ResponseEntity<org.springframework.core.io.Resource> preview(
      @PathVariable @Positive Long mediaId) {
    return binary(mediaService.load(mediaId), false);
  }

  @GetMapping("/{mediaId}/download")
  public ResponseEntity<org.springframework.core.io.Resource> download(
      @PathVariable @Positive Long mediaId) {
    return binary(mediaService.load(mediaId), true);
  }

  private ResponseEntity<org.springframework.core.io.Resource> binary(
      MediaBinary file, boolean attachment) {
    ContentDisposition disposition =
        (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
            .filename(file.getOriginalName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(org.springframework.http.MediaType.parseMediaType(file.getMimeType()))
        .contentLength(file.getFileSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(file.getResource());
  }
}
