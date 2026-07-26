package com.example.LMS_BE.student.controller;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.dto.PageResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.student.dto.StudentRequest;
import com.example.LMS_BE.student.dto.StudentResponse;
import com.example.LMS_BE.student.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
  private final StudentService service;
  private final MessageUtils messages;

  @GetMapping
  public ApiResponse<PageResponse<StudentResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "20") @Positive int size) {
    return ApiResponse.success(messages.get("common.success"), service.search(keyword, page, size));
  }

  @GetMapping("/{id}")
  public ApiResponse<StudentResponse> detail(@PathVariable @Positive Long id) {
    return ApiResponse.success(messages.get("common.success"), service.detail(id));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<StudentResponse>> create(
      @Valid @RequestPart("data") StudentRequest request,
      @RequestPart(required = false) MultipartFile avatar,
      @RequestPart(required = false) List<MultipartFile> avatars) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                messages.get("student.created"), service.create(request, avatar, avatars)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<StudentResponse> update(
      @PathVariable @Positive Long id,
      @Valid @RequestPart("data") StudentRequest request,
      @RequestPart(required = false) MultipartFile avatar,
      @RequestPart(required = false) List<MultipartFile> avatars) {
    return ApiResponse.success(
        messages.get("student.updated"), service.update(id, request, avatar, avatars));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable @Positive Long id) {
    service.delete(id);
    return ApiResponse.success(messages.get("student.deleted"), null);
  }

  @GetMapping(
      value = "/export",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> export(@RequestParam(required = false) String keyword) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.xlsx")
        .body(service.export(keyword));
  }
}
