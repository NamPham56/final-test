package com.example.LMS_BE.lesson.controller;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.lesson.dto.LessonRequest;
import com.example.LMS_BE.lesson.dto.LessonResponse;
import com.example.LMS_BE.lesson.service.LessonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {
  private final LessonService service;
  private final MessageUtils messages;

  @GetMapping
  public ApiResponse<List<LessonResponse>> list(@RequestParam Long courseId) {
    return ApiResponse.success(messages.get("common.success"), service.list(courseId));
  }

  @GetMapping("/{id}")
  public ApiResponse<LessonResponse> detail(@PathVariable @Positive Long id) {
    return ApiResponse.success(messages.get("common.success"), service.detail(id));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<LessonResponse>> create(
      @Valid @RequestPart("data") LessonRequest request,
      @RequestPart(required = false) MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> thumbnails,
      @RequestPart(required = false) MultipartFile video,
      @RequestPart(required = false) List<MultipartFile> videos,
      @RequestPart(required = false) List<MultipartFile> images) {
    return ResponseEntity.status(201)
        .body(
            ApiResponse.success(
                messages.get("lesson.created"),
                service.create(request, thumbnail, thumbnails, video, videos, images)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<LessonResponse> update(
      @PathVariable Long id,
      @Valid @RequestPart("data") LessonRequest request,
      @RequestPart(required = false) MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> thumbnails,
      @RequestPart(required = false) MultipartFile video,
      @RequestPart(required = false) List<MultipartFile> videos,
      @RequestPart(required = false) List<MultipartFile> images) {
    return ApiResponse.success(
        messages.get("lesson.updated"),
        service.update(id, request, thumbnail, thumbnails, video, videos, images));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ApiResponse.success(messages.get("lesson.deleted"), null);
  }
}
