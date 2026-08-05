package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record SkillRequest(
    @NotBlank(message = "Name cannot be empty") @Size(max = 255, message = "Name must not exceed 255 characters") String name,
    @NotBlank(message = "Category cannot be empty") @Pattern(
            regexp = "programming_language|framework|database|tool|other",
            message =
                "Category must be one of: programming_language, framework, database, tool, other")
        String category,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    @NotEmpty(message = "At least the default locale (en) translation must be provided")
        List<@Valid SkillTranslationRequest> translations) {

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
            .map(SkillTranslationRequest::locale)
            .distinct()
            .count();
    return distinctCount == translations.size();
  }
}
