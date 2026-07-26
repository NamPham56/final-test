package com.example.LMS_BE.media.mapper;

import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.entity.MediaFile;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MediaMapper {
  @Mapping(target = "mediaId", source = "media.id")
  @Mapping(target = "mediaType", source = "mediaType")
  MediaResponse toResponse(MediaFile media, MediaType mediaType);
}
