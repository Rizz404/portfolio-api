package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Project.LinkType;
import com.api.rizz.portfolio_api.entity.Project.ProjectStatus;
import com.api.rizz.portfolio_api.entity.Project.ProjectType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.hibernate.validator.constraints.URL;

public record ProjectRequest(
    @NotEmpty(message = "At least the default locale (en) translation must be provided")
        List<@Valid ProjectTranslationRequest> translations,
    @NotNull(message = "Status must be provided") ProjectStatus status,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    List<String> imageUrls,
    Map<String, String> techStack,
    List<ProjectType> projectTypes,
    Map<LinkType, String> projectLinks,
    List<String> deletedImageUrls) {

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
            .map(ProjectTranslationRequest::locale)
            .distinct()
            .count();
    return distinctCount == translations.size();
  }
}
