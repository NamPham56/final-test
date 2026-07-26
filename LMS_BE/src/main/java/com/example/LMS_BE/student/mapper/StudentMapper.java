package com.example.LMS_BE.student.mapper;

import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.student.dto.StudentRequest;
import com.example.LMS_BE.student.dto.StudentResponse;
import com.example.LMS_BE.student.enity.Student;
import java.util.List;
import java.util.Map;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring")
public interface StudentMapper {
  @Mapping(
      target = "media",
      expression = "java(mediaByObject.getOrDefault(entity.getId(), java.util.List.of()))")
  StudentResponse toResponse(Student entity, @Context Map<Long, List<MediaResponse>> mediaByObject);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Student toEntity(StudentRequest request);

  @BeanMapping(
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
      unmappedTargetPolicy = ReportingPolicy.IGNORE)
  void updateEntity(StudentRequest request, @MappingTarget Student entity);
}
