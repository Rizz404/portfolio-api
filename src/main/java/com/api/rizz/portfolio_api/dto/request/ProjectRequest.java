package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.hibernate.validator.constraints.URL;

public record ProjectRequest(
    @NotBlank(message = "Project name cannot be empty") @Size(max = 255, message = "Project name must not exceed 255 characters") String name,
    String description,
    @NotBlank(message = "Status must be provided") @Size(max = 50, message = "Status must not exceed 50 characters") String status,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    List<String> imageUrls,
    Map<String, String> projectLinks,
    List<String> deletedImageUrls) {}
