package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record SkillRequest(
    @NotBlank(message = "Name cannot be empty") @Size(max = 255, message = "Name must not exceed 255 characters") String name,
    @NotBlank(message = "Category cannot be empty") @Pattern(
            regexp = "programming_language|framework|database|tool|other",
            message =
                "Category must be one of: programming_language, framework, database, tool, other")
        String category,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    String description) {}
