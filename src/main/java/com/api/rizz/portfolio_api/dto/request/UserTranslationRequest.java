package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.constraints.NotNull;

public record UserTranslationRequest(
    @NotNull(message = "Locale must be provided") LanguageCode locale, String bio) {}
