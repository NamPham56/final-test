package com.example.LMS_BE.lesson.mapper;

import com.example.LMS_BE.lesson.dto.*;
import com.example.LMS_BE.lesson.entity.Lesson;
import com.example.LMS_BE.media.dto.MediaResponse;
import java.util.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LessonMapper {
  @Mapping(target = "courseId", source = "course.id")
  @Mapping(
      target = "media",
      expression = "java(mediaByObject.getOrDefault(entity.getId(), java.util.List.of()))")
  LessonResponse toResponse(Lesson entity, @Context Map<Long, List<MediaResponse>> mediaByObject);

  @Mapping(target = "course", ignore = true)
  @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  Lesson toEntity(LessonRequest request);

  @Mapping(target = "course", ignore = true)
  @BeanMapping(
      nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
      unmappedTargetPolicy = ReportingPolicy.IGNORE)
  void updateEntity(LessonRequest request, @MappingTarget Lesson entity);
}
