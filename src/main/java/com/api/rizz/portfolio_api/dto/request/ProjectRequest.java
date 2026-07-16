package com.api.rizz.portfolio_api.dto.request;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "Project name cannot be empty") String name,
        String description,
        @NotBlank(message = "Status must be provided") String status,
        String logoUrl,
        List<String> imageUrls,
        Map<String, String> projectLinks,
        List<String> deletedImageUrls) {
}
