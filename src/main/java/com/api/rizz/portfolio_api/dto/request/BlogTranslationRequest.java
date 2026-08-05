package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlogTranslationRequest(
    @NotNull(message = "Locale must be provided") LanguageCode locale,
    @NotBlank(message = "Blog title cannot be empty") @Size(max = 255, message = "Blog title must not exceed 255 characters") String title,
    @NotBlank(message = "Blog content cannot be empty") String content) {}
