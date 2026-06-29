package com.api.rizz.portfolio_api.entity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Project
 */
public class Project {
  @Id
  private Long id;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 50)
  private String status;

  @Column(name = "logo_url")
  private String logoUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "image_urls", columnDefinition = "jsonb")
  private List<String> imageUrls;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "project_links", columnDefinition = "jsonb")
  Map<String, String> projectLinks; // * Biar key value pair

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private OffsetDateTime createdAt;

  @CreationTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
