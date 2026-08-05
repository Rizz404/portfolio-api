package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
    @NotBlank(message = "Company name cannot be empty") @Size(max = 255, message = "Company name must not exceed 255 characters") String companyName,
    @NotNull(message = "Start date must be filled") @PastOrPresent(message = "Start date cannot be in the future") LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent,
    @NotEmpty(message = "At least the default locale (en) translation must be provided")
        List<@Valid ExperienceTranslationRequest> translations) {

  @AssertTrue(message = "End date cannot be before start date") public boolean isEndDateValid() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  @AssertTrue(message = "End date must be empty when the experience is marked as current") public boolean isCurrentExperienceValid() {
    return !Boolean.TRUE.equals(isCurrent) || endDate == null;
  }

  @AssertTrue(message = "Default locale (en) translation must be provided")
  public boolean isDefaultLocalePresent() {
    return translations == null
        || translations.stream().anyMatch(t -> t != null && t.locale() == LanguageCode.en);
  }

  @AssertTrue(message = "Each locale can only appear once in translations") public boolean isLocaleUnique() {
    if (translations == null) return true;
    long distinctCount =
        translations.stream()
            .filter(t -> t != null && t.locale() != null)
            .map(ExperienceTranslationRequest::locale)
            .distinct()
            .count();
    return distinctCount == translations.size();
  }
}
