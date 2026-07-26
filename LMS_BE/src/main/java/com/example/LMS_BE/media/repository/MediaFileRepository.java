package com.example.LMS_BE.media.repository;

import com.example.LMS_BE.media.entity.MediaFile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
  Optional<MediaFile> findByIdAndStatus(Long id, Integer status);

  List<MediaFile> findAllByIdInAndStatus(Collection<Long> ids, Integer status);
}
