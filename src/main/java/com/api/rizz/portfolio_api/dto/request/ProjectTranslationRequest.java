package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectTranslationRequest(
    @NotNull(message = "Locale must be provided") LanguageCode locale,
    @NotBlank(message = "Project name cannot be empty") @Size(max = 255, message = "Project name must not exceed 255 characters") String name,
    String description) {}
