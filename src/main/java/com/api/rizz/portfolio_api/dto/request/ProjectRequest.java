package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.Project.LinkType;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import com.api.rizz.portfolio_api.entity.Project.ProjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.hibernate.validator.constraints.URL;

public record ProjectRequest(
    @NotBlank(message = "Project name cannot be empty") @Size(max = 255, message = "Project name must not exceed 255 characters") String name,
    String description,
    @NotNull(message = "Status must be provided") ProjectStatus status,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    List<String> imageUrls,
    Map<String, String> techStack,
    List<ProjectType> projectTypes,
    Map<LinkType, String> projectLinks,
    List<String> deletedImageUrls) {}
