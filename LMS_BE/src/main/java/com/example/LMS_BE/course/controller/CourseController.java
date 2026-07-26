package com.example.LMS_BE.course.controller;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.course.dto.CourseRequest;
import com.example.LMS_BE.course.dto.CourseResponse;
import com.example.LMS_BE.course.service.CourseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
  private final CourseService service;
  private final MessageUtils messages;

  @GetMapping
  public ApiResponse<PageResponse<CourseResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Positive int size) {
    return ApiResponse.success(
        messages.get("common.success"), service.search(keyword, fromDate, toDate, page, size));
  }

  @GetMapping("/{id}")
  public ApiResponse<CourseResponse> detail(@PathVariable @Positive Long id) {
    return ApiResponse.success(messages.get("common.success"), service.detail(id));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<CourseResponse>> create(
      @Valid @RequestPart("data") CourseRequest request,
      @RequestPart(required = false) MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> thumbnails,
      @RequestPart(required = false) List<MultipartFile> images,
      @RequestPart(required = false) List<MultipartFile> videos) {
    return ResponseEntity.status(201)
        .body(
            ApiResponse.success(
                messages.get("course.created"),
                service.create(request, thumbnail, thumbnails, images, videos)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<CourseResponse> update(
      @PathVariable @Positive Long id,
      @Valid @RequestPart("data") CourseRequest request,
      @RequestPart(required = false) MultipartFile thumbnail,
      @RequestPart(required = false) List<MultipartFile> thumbnails,
      @RequestPart(required = false) List<MultipartFile> images,
      @RequestPart(required = false) List<MultipartFile> videos) {
    return ApiResponse.success(
        messages.get("course.updated"),
        service.update(id, request, thumbnail, thumbnails, images, videos));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
    service.delete(id);
    return ApiResponse.success(messages.get("course.deleted"), null);
  }

  @GetMapping(
      value = "/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> export(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=courses.xlsx")
        .body(service.export(keyword, fromDate, toDate));
  }
}
