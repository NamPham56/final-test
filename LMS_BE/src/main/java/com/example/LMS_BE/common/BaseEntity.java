package com.example.LMS_BE.common;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 1: đang hoạt động 0: đã xóa =0 */
  @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
  private Integer status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void prePersist() {
    LocalDateTime now = LocalDateTime.now();

    if (status == null) {
      status = 1;
    }

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  protected void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public void softDelete() {
    this.status = 0;
  }

  public void restore() {
    this.status = 1;
  }

  public boolean isActive() {
    return Integer.valueOf(1).equals(status);
  }

  public boolean isDeleted() {
    return Integer.valueOf(0).equals(status);
  }
}
