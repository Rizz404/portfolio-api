package com.api.rizz.portfolio_api.entity;

import java.time.OffsetDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "blog_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * BlogAttachment
 */
public class BlogAttachment {
  @Id
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "blog_id", nullable = false)
  private Blog blog;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_url", nullable = false)
  private String fileUrl;

  @Column(name = "file_type", nullable = false, length = 100)
  private String fileType;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
