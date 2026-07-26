package com.example.LMS_BE.media.service;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.ErrorCode;
import com.example.LMS_BE.common.exception.FileStorageException;
import com.example.LMS_BE.common.exception.ResourceNotFoundException;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import com.example.LMS_BE.media.dto.MediaBinary;
import com.example.LMS_BE.media.dto.MediaResponse;
import com.example.LMS_BE.media.entity.MediaFile;
import com.example.LMS_BE.media.entity.ObjectMedia;
import com.example.LMS_BE.media.mapper.MediaMapper;
import com.example.LMS_BE.media.repository.MediaFileRepository;
import com.example.LMS_BE.media.repository.ObjectMediaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class MediaService {
  private final MediaFileRepository mediaFileRepository;
  private final ObjectMediaRepository objectMediaRepository;
  private final MediaMapper mediaMapper;

  @Value("${app.upload-dir:uploads}")
  private String uploadDir;

  @Transactional
  public MediaFile store(MultipartFile file) {
    if (file == null || file.isEmpty()) return null;
    String originalName = validatedOriginalName(file.getOriginalFilename());
    String extension =
        originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1) : "";
    if (extension.length() > 20) throw new BusinessException(ErrorCode.FILE_INVALID_NAME);
    String mimeType = validatedMimeType(file.getContentType());
    long reportedSize = file.getSize();
    if (reportedSize <= 0) throw new BusinessException(ErrorCode.FILE_INVALID_SIZE);
    String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
    try {
      Path directory = uploadDirectory();
      Files.createDirectories(directory);
      Path target = directory.resolve(storedName).normalize();
      if (!target.startsWith(directory)) throw new BusinessException(ErrorCode.FILE_INVALID_NAME);
      file.transferTo(target);
      if (!Files.isRegularFile(target) || Files.size(target) != reportedSize) {
        Files.deleteIfExists(target);
        throw new BusinessException(ErrorCode.FILE_INVALID_SIZE);
      }
      MediaFile media = new MediaFile();
      media.setOriginalName(originalName);
      media.setStoredName(storedName);
      media.setFilePath(target.toString());
      media.setFileUrl(null);
      media.setMimeType(mimeType);
      media.setExtension(extension);
      media.setFileSize(reportedSize);
      return mediaFileRepository.save(media);
    } catch (IOException | IllegalStateException | SecurityException e) {
      throw new FileStorageException(ErrorCode.FILE_STORAGE_ERROR, e);
    }
  }

  @Transactional
  public MediaResponse upload(MultipartFile file, MediaType mediaType) {
    MediaFile stored = store(file);
    if (stored == null) throw new BusinessException(ErrorCode.FILE_EMPTY);
    return mediaMapper.toResponse(stored, mediaType);
  }

  @Transactional
  public void attach(ObjectType objectType, Long objectId, MediaFile media, MediaType mediaType) {
    if (media == null) return;
    ObjectMedia link = new ObjectMedia();
    link.setObjectType(objectType);
    link.setObjectId(objectId);
    link.setMediaFile(media);
    link.setMediaType(mediaType);
    // Database enforces display_order > 0.
    link.setDisplayOrder(1);
    link.setIsPrimary(1);
    objectMediaRepository.save(link);
  }

  @Transactional
  public void attachUploaded(
      ObjectType objectType, Long objectId, Collection<MultipartFile> files, MediaType mediaType) {
    if (files == null || files.isEmpty()) return;
    List<ObjectMedia> links = new ArrayList<>();
    int displayOrder = 1;
    for (MultipartFile file : files) {
      MediaFile media = store(file);
      if (media == null) continue;
      ObjectMedia link = new ObjectMedia();
      link.setObjectType(objectType);
      link.setObjectId(objectId);
      link.setMediaFile(media);
      link.setMediaType(mediaType);
      link.setDisplayOrder(displayOrder++);
      link.setIsPrimary(links.isEmpty() ? 1 : 0);
      links.add(link);
    }
    if (!links.isEmpty()) objectMediaRepository.saveAll(links);
  }

  /** Đồng bộ theo ID, không xóa rồi tạo lại các liên kết đang được giữ. */
  @Transactional
  public void sync(
      ObjectType type,
      Long objectId,
      List<Long> retainedIds,
      List<Long> newIds,
      List<Long> removedIds,
      MediaType defaultType) {
    List<ObjectMedia> currentLinks = objectMediaRepository.findAllLinks(type, objectId);
    Map<Long, ObjectMedia> byMediaId =
        currentLinks.stream()
            .collect(
                Collectors.toMap(x -> x.getMediaFile().getId(), Function.identity(), (a, b) -> a));
    Set<Long> retained = set(retainedIds);
    Set<Long> removed = set(removedIds);
    if (!Collections.disjoint(retained, removed))
      throw new BusinessException(ErrorCode.MEDIA_CONFLICTING_CHANGES);

    for (Long id : retained) {
      ObjectMedia link = byMediaId.get(id);
      if (link == null) throw new BusinessException(ErrorCode.MEDIA_RETAINED_NOT_LINKED, id);
      if (!link.isActive()) link.restore();
      if (!link.getMediaFile().isActive()) link.getMediaFile().restore();
    }
    for (Long id : removed) {
      ObjectMedia link = byMediaId.get(id);
      if (link == null) throw new BusinessException(ErrorCode.MEDIA_REMOVED_NOT_LINKED, id);
      link.softDelete();
    }
    Set<Long> newMediaIds = set(newIds);
    Map<Long, MediaFile> newMediaById =
        newMediaIds.isEmpty()
            ? Map.of()
            : mediaFileRepository.findAllByIdInAndStatus(newMediaIds, 1).stream()
                .collect(Collectors.toMap(MediaFile::getId, Function.identity()));
    List<ObjectMedia> newLinks = new ArrayList<>();
    int nextDisplayOrder =
        currentLinks.stream()
                .map(ObjectMedia::getDisplayOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0)
            + 1;
    for (Long id : newMediaIds) {
      MediaFile media = newMediaById.get(id);
      if (media == null) {
        throw new ResourceNotFoundException(ErrorCode.MEDIA_NOT_FOUND_WITH_ID, id);
      }
      ObjectMedia existing = byMediaId.get(id);
      if (existing != null) {
        existing.restore();
      } else {
        newLinks.add(newLink(type, objectId, media, defaultType, nextDisplayOrder++));
      }
    }
    objectMediaRepository.saveAll(currentLinks);
    if (!newLinks.isEmpty()) {
      objectMediaRepository.saveAll(newLinks);
    }
    softDeleteOrphanFiles(removed);
  }

  public Map<Long, List<MediaResponse>> findByObjects(ObjectType type, Collection<Long> objectIds) {
    if (objectIds == null || objectIds.isEmpty()) return Map.of();
    return objectMediaRepository.findActiveByObjects(type, objectIds).stream()
        .collect(
            Collectors.groupingBy(
                ObjectMedia::getObjectId,
                LinkedHashMap::new,
                Collectors.mapping(this::toResponse, Collectors.toList())));
  }

  public MediaBinary load(Long mediaId) {
    MediaFile media =
        mediaFileRepository
            .findByIdAndStatus(mediaId, 1)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MEDIA_NOT_FOUND));
    String originalName = validatedOriginalName(media.getOriginalName());
    String mimeType = validatedMimeType(media.getMimeType());
    Long expectedSize = media.getFileSize();
    if (expectedSize == null || expectedSize <= 0)
      throw new BusinessException(ErrorCode.FILE_INVALID_SIZE);

    Path path = storedPath(media.getFilePath());
    if (!Files.isRegularFile(path))
      throw new ResourceNotFoundException(ErrorCode.FILE_PHYSICAL_MISSING, originalName);
    try {
      if (Files.size(path) != expectedSize)
        throw new BusinessException(ErrorCode.FILE_INVALID_SIZE);
    } catch (IOException | SecurityException e) {
      throw new FileStorageException(ErrorCode.FILE_STORAGE_ERROR, e);
    }
    return new MediaBinary(new FileSystemResource(path), originalName, mimeType, expectedSize);
  }

  private String validatedOriginalName(String value) {
    if (value == null) throw new BusinessException(ErrorCode.FILE_INVALID_NAME);
    String name = value.trim();
    if (name.isBlank()
        || name.length() > 255
        || name.indexOf('\0') >= 0
        || name.indexOf('\r') >= 0
        || name.indexOf('\n') >= 0
        || name.contains("/")
        || name.contains("\\")) {
      throw new BusinessException(ErrorCode.FILE_INVALID_NAME);
    }
    return name;
  }

  private String validatedMimeType(String value) {
    String mimeType = value == null || value.isBlank() ? "application/octet-stream" : value.trim();
    if (mimeType.length() > 100) throw new BusinessException(ErrorCode.FILE_INVALID_MIME_TYPE);
    try {
      return org.springframework.http.MediaType.parseMediaType(mimeType).toString();
    } catch (InvalidMediaTypeException exception) {
      throw new BusinessException(ErrorCode.FILE_INVALID_MIME_TYPE);
    }
  }

  private Path uploadDirectory() {
    if (uploadDir == null || uploadDir.isBlank())
      throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
    try {
      return Path.of(uploadDir).toAbsolutePath().normalize();
    } catch (InvalidPathException | SecurityException exception) {
      throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
    }
  }

  private Path storedPath(String value) {
    if (value == null || value.isBlank()) throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
    try {
      Path path = Path.of(value);
      if (!path.isAbsolute()) throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
      path = path.normalize();
      if (!path.startsWith(uploadDirectory()))
        throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
      return path;
    } catch (InvalidPathException | SecurityException exception) {
      throw new BusinessException(ErrorCode.FILE_INVALID_PATH);
    }
  }

  private void softDeleteOrphanFiles(Set<Long> mediaIds) {
    if (mediaIds.isEmpty()) {
      return;
    }
    Set<Long> activeMediaIds = objectMediaRepository.findActiveMediaFileIds(mediaIds);
    Set<Long> orphanIds = new LinkedHashSet<>(mediaIds);
    orphanIds.removeAll(activeMediaIds);
    if (orphanIds.isEmpty()) {
      return;
    }
    List<MediaFile> orphanFiles = mediaFileRepository.findAllById(orphanIds);
    orphanFiles.forEach(MediaFile::softDelete);
    mediaFileRepository.saveAll(orphanFiles);
  }

  private ObjectMedia newLink(
      ObjectType objectType,
      Long objectId,
      MediaFile media,
      MediaType mediaType,
      int displayOrder) {
    ObjectMedia link = new ObjectMedia();
    link.setObjectType(objectType);
    link.setObjectId(objectId);
    link.setMediaFile(media);
    link.setMediaType(mediaType);
    link.setDisplayOrder(displayOrder);
    link.setIsPrimary(displayOrder == 1 ? 1 : 0);
    return link;
  }

  private MediaResponse toResponse(ObjectMedia link) {
    MediaFile media = link.getMediaFile();
    return mediaMapper.toResponse(media, link.getMediaType());
  }

  private Set<Long> set(List<Long> ids) {
    return ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
  }
}
