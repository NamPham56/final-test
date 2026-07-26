package com.example.LMS_BE.media.repository;

import com.example.LMS_BE.media.constant.*;
import com.example.LMS_BE.media.entity.ObjectMedia;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObjectMediaRepository extends JpaRepository<ObjectMedia, Long> {
  List<ObjectMedia> findAllByObjectTypeAndObjectIdAndStatusOrderByDisplayOrder(
      ObjectType type, Long objectId, Integer status);

  Optional<ObjectMedia> findByObjectTypeAndObjectIdAndMediaFileId(
      ObjectType type, Long objectId, Long mediaId);

  boolean existsByMediaFileIdAndStatus(Long mediaId, Integer status);

  @Query(
      """
      select om
      from ObjectMedia om
      join fetch om.mediaFile mf
      where om.objectType = :type
        and om.objectId in :objectIds
        and om.status = 1
        and mf.status = 1
      order by om.objectId, om.displayOrder
      """)
  List<ObjectMedia> findActiveByObjects(
      @Param("type") ObjectType type, @Param("objectIds") Collection<Long> objectIds);

  @Query(
      """
      select om
      from ObjectMedia om
      join fetch om.mediaFile mf
      where om.objectType = :type
        and om.objectId = :objectId
      """)
  List<ObjectMedia> findAllLinks(@Param("type") ObjectType type, @Param("objectId") Long objectId);

  @Query(
      """
      select distinct om.mediaFile.id
      from ObjectMedia om
      where om.status = 1
        and om.mediaFile.id in :mediaIds
      """)
  Set<Long> findActiveMediaFileIds(@Param("mediaIds") Collection<Long> mediaIds);
}
