package com.example.LMS_BE.course.mapper;

import com.example.LMS_BE.course.dto.*;
import com.example.LMS_BE.course.entity.Course;
import com.example.LMS_BE.media.dto.MediaResponse;
import java.util.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CourseMapper {
  @Mapping(
      target = "media",
      expression = "java(mediaByObject.getOrDefault(entity.getId(), java.util.List.of()))")
  CourseResponse toResponse(Course entity, @Context Map<Long, List<MediaResponse>> mediaByObject);

  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Course toEntity(CourseRequest request);

  @BeanMapping(
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
      unmappedTargetPolicy = ReportingPolicy.IGNORE)
  void updateEntity(CourseRequest request, @MappingTarget Course entity);
}
