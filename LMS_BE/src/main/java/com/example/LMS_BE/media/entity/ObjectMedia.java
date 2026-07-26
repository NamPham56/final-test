package com.example.LMS_BE.media.entity;

import com.example.LMS_BE.common.BaseEntity;
import com.example.LMS_BE.media.constant.MediaType;
import com.example.LMS_BE.media.constant.ObjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "object_media")
public class ObjectMedia extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(name = "object_type", nullable = false, length = 50)
  private ObjectType objectType;

  @Column(name = "object_id", nullable = false)
  private Long objectId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "media_id", nullable = false)
  private MediaFile mediaFile;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 30)
  private MediaType mediaType;

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder;

  @Column(name = "is_primary", nullable = false, columnDefinition = "TINYINT")
  private Integer isPrimary;
}
