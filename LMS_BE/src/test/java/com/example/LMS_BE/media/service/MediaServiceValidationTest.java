package com.example.LMS_BE.media.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import com.example.LMS_BE.media.entity.MediaFile;
import com.example.LMS_BE.media.entity.ObjectMedia;
import com.example.LMS_BE.media.mapper.MediaMapper;
import com.example.LMS_BE.media.repository.MediaFileRepository;
import com.example.LMS_BE.media.repository.ObjectMediaRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class MediaServiceValidationTest {
  @TempDir Path uploadDirectory;

  @Test
  void storeRejectsMalformedMimeType() {
    MediaService service = service(mock(MediaFileRepository.class));
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.png", "not a mime type", new byte[] {1});

    assertThatThrownBy(() -> service.store(file))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.FILE_INVALID_MIME_TYPE));
  }

  @Test
  void loadRejectsStoredPathOutsideConfiguredUploadDirectory() {
    MediaFileRepository repository = mock(MediaFileRepository.class);
    MediaService service = service(repository);
    MediaFile media = media(Path.of("outside", "file.png").toAbsolutePath().toString(), 1L);
    when(repository.findByIdAndStatus(1L, 1)).thenReturn(Optional.of(media));

    assertThatThrownBy(() -> service.load(1L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                    .isEqualTo(ErrorCode.FILE_INVALID_PATH));
  }

  @Test
  void syncRemovesOldMediaKeepsRetainedMediaAndAddsNewMediaWithoutRecreatingKeptLinks() {
    MediaFileRepository mediaRepository = mock(MediaFileRepository.class);
    ObjectMediaRepository objectMediaRepository = mock(ObjectMediaRepository.class);
    MediaService service = service(mediaRepository, objectMediaRepository);
    ObjectMedia oldA = link(1L, MediaType.IMAGE);
    ObjectMedia oldB = link(2L, MediaType.IMAGE);
    ObjectMedia oldC = link(3L, MediaType.VIDEO);
    MediaFile newD = media(4L);

    when(objectMediaRepository.findAllLinks(ObjectType.COURSE, 10L))
        .thenReturn(List.of(oldA, oldB, oldC));
    when(mediaRepository.findAllByIdInAndStatus(Set.of(4L), 1)).thenReturn(List.of(newD));
    when(objectMediaRepository.findActiveMediaFileIds(Set.of(1L))).thenReturn(Set.of(1L));

    service.sync(
        ObjectType.COURSE, 10L, List.of(2L, 3L), List.of(4L), List.of(1L), MediaType.IMAGE);

    assertThat(oldA.isDeleted()).isTrue();
    assertThat(oldB.isActive()).isTrue();
    assertThat(oldC.isActive()).isTrue();
    verify(mediaRepository).findAllByIdInAndStatus(Set.of(4L), 1);
    verify(mediaRepository, never()).findByIdAndStatus(4L, 1);
    verify(objectMediaRepository).saveAll(List.of(oldA, oldB, oldC));
    verify(objectMediaRepository)
        .saveAll(
            org.mockito.ArgumentMatchers.<List<ObjectMedia>>argThat(
                links -> links.size() == 1 && links.get(0).getMediaFile().getId().equals(4L)));
  }

  @Test
  void syncSoftDeletesAnOrphanedPhysicalMediaFileWithOneBatchLookup() {
    MediaFileRepository mediaRepository = mock(MediaFileRepository.class);
    ObjectMediaRepository objectMediaRepository = mock(ObjectMediaRepository.class);
    MediaService service = service(mediaRepository, objectMediaRepository);
    ObjectMedia oldA = link(1L, MediaType.IMAGE);
    MediaFile physicalA = oldA.getMediaFile();

    when(objectMediaRepository.findAllLinks(ObjectType.COURSE, 10L)).thenReturn(List.of(oldA));
    when(objectMediaRepository.findActiveMediaFileIds(Set.of(1L))).thenReturn(Set.of());
    when(mediaRepository.findAllById(Set.of(1L))).thenReturn(List.of(physicalA));

    service.sync(ObjectType.COURSE, 10L, List.of(), List.of(), List.of(1L), MediaType.IMAGE);

    assertThat(oldA.isDeleted()).isTrue();
    assertThat(physicalA.isDeleted()).isTrue();
    verify(objectMediaRepository).findActiveMediaFileIds(Set.of(1L));
    verify(mediaRepository).findAllById(Set.of(1L));
    verify(mediaRepository).saveAll(List.of(physicalA));
    verify(objectMediaRepository, never()).existsByMediaFileIdAndStatus(1L, 1);
  }

  private MediaService service(MediaFileRepository repository) {
    return service(repository, mock(ObjectMediaRepository.class));
  }

  private MediaService service(
      MediaFileRepository repository, ObjectMediaRepository objectMediaRepository) {
    MediaService service =
        new MediaService(repository, objectMediaRepository, mock(MediaMapper.class));
    ReflectionTestUtils.setField(service, "uploadDir", uploadDirectory.toString());
    return service;
  }

  private MediaFile media(String path, long size) {
    MediaFile media = new MediaFile();
    media.setId(1L);
    media.setStatus(1);
    media.setOriginalName("file.png");
    media.setFilePath(path);
    media.setMimeType("image/png");
    media.setFileSize(size);
    return media;
  }

  private MediaFile media(Long id) {
    MediaFile media = media(uploadDirectory.resolve("file-" + id + ".png").toString(), 1L);
    media.setId(id);
    return media;
  }

  private ObjectMedia link(Long mediaId, MediaType mediaType) {
    ObjectMedia link = new ObjectMedia();
    link.setStatus(1);
    link.setObjectType(ObjectType.COURSE);
    link.setObjectId(10L);
    link.setMediaFile(media(mediaId));
    link.setMediaType(mediaType);
    link.setDisplayOrder(mediaId.intValue());
    link.setIsPrimary(mediaId == 1L ? 1 : 0);
    return link;
  }
}
