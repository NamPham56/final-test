package com.example.LMS_BE.enrollment.mapper;

import com.example.LMS_BE.enrollment.dto.*;
import com.example.LMS_BE.enrollment.entity.Enrollment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {
  @Mapping(target = "studentId", source = "student.id")
  @Mapping(target = "studentCode", source = "student.studentCode")
  @Mapping(target = "studentName", source = "student.fullName")
  @Mapping(target = "courseId", source = "course.id")
  @Mapping(target = "courseCode", source = "course.courseCode")
  @Mapping(target = "courseName", source = "course.courseName")
  EnrollmentResponse toResponse(Enrollment entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "student", ignore = true)
  @Mapping(target = "course", ignore = true)
  @Mapping(target = "enrolledAt", ignore = true)
  @Mapping(target = "completedAt", ignore = true)
  @Mapping(target = "enrollmentStatus", ignore = true)
  @Mapping(target = "progressPercent", ignore = true)
  Enrollment toEntity(EnrollmentRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "student", ignore = true)
  @Mapping(target = "course", ignore = true)
  @Mapping(target = "enrolledAt", ignore = true)
  @Mapping(target = "completedAt", ignore = true)
  void updateEntity(EnrollmentUpdateRequest request, @MappingTarget Enrollment entity);
}
