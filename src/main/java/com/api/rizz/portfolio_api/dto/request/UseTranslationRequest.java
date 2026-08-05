package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.constraints.NotNull;

public record UseTranslationRequest(
    @NotNull(message = "Locale must be provided") LanguageCode locale, String reasons) {}
