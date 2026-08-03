package com.api.rizz.portfolio_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/** Project */
public class Project {

  public enum ProjectStatus {
    active,
    inactive,
    development,
    maintenance,
    archived
  }

  public enum ProjectType {
    frontend,
    backend,
    fullstack,
    mobile,
    desktop,
    api,
    library,
    other
  }

  public enum LinkType {
    github,
    gitlab,
    bitbucket,
    source_code,
    demo,
    website,
    figma,
    documentation,
    api_docs,
    video,
    playstore,
    appstore,
    npm,
    dockerhub,
    staging,
    other
  }

  @Id private Long id;

  @Column(nullable = false, unique = true)
  private String slug;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private ProjectStatus status;

  @Column(name = "logo_url")
  private String logoUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "image_urls", columnDefinition = "jsonb")
  private List<String> imageUrls;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tech_stack", columnDefinition = "jsonb")
  private Map<String, String> techStack; // * Key = nama tech, Value = logo URL

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "project_types", columnDefinition = "jsonb")
  private List<ProjectType> projectTypes; // * Frontend, backend, mobile, dll (bisa lebih dari satu)

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "project_links", columnDefinition = "jsonb")
  Map<LinkType, String> projectLinks; // * Biar key value pair

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
