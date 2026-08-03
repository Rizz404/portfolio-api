package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.Project.LinkType;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import com.api.rizz.portfolio_api.entity.Project.ProjectType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProjectResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id, // ID diubah
    // jadi string
    // biar
    // snowflakes
    // gak error
    String slug,
    String name,
    String description,
    ProjectStatus status,
    String logoUrl,
    List<String> imageUrls,
    Map<LinkType, String> projectLinks,
    Map<String, String> techStack,
    List<ProjectType> projectTypes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
