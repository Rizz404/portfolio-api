package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ExperienceTranslationRequest(
    @NotNull(message = "Locale must be provided") LanguageCode locale,
    @NotBlank(message = "Position cannot be empty") @Size(max = 255, message = "Position must not exceed 255 characters") String position,
    String description,
    List<String> jobdesks) {}
