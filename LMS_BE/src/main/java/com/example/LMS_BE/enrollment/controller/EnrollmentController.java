package com.example.LMS_BE.enrollment.controller;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.enrollment.constant.EnrollmentStatus;
import com.example.LMS_BE.enrollment.dto.EnrollmentRequest;
import com.example.LMS_BE.enrollment.dto.EnrollmentResponse;
import com.example.LMS_BE.enrollment.dto.EnrollmentUpdateRequest;
import com.example.LMS_BE.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
  private final EnrollmentService service;
  private final MessageUtils messages;

  @GetMapping
  public ApiResponse<PageResponse<EnrollmentResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) EnrollmentStatus status,
      @RequestParam(required = false) @Positive Long courseId,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Positive int size) {
    return ApiResponse.success(
        messages.get("common.success"), service.search(keyword, status, courseId, page, size));
  }

  @GetMapping("/{id}")
  public ApiResponse<EnrollmentResponse> detail(@PathVariable @Positive Long id) {
    return ApiResponse.success(messages.get("common.success"), service.detail(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> enroll(
      @Valid @RequestBody EnrollmentRequest request) {
    return ResponseEntity.status(201)
        .body(ApiResponse.success(messages.get("enrollment.created"), service.enroll(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<EnrollmentResponse> update(
      @PathVariable @Positive Long id, @Valid @RequestBody EnrollmentUpdateRequest request) {
    return ApiResponse.success(messages.get("enrollment.updated"), service.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
    service.delete(id);
    return ApiResponse.success(messages.get("enrollment.deleted"), null);
  }

  @GetMapping("/course/{courseId}/students")
  public ApiResponse<List<EnrollmentResponse>> students(@PathVariable @Positive Long courseId) {
    return ApiResponse.success(messages.get("common.success"), service.students(courseId));
  }
}
