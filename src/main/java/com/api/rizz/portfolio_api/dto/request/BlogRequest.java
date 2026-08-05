package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.LanguageCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record BlogRequest(
    Boolean isPublished,
    @URL(message = "Featured image URL must be a valid URL") String featuredImageUrl,
    @PositiveOrZero(message = "Views count cannot be negative") int viewsCount,
    @PositiveOrZero(message = "Likes count cannot be negative") int likesCount,
    @PositiveOrZero(message = "Dislikes count cannot be negative") int dislikesCount,
    List<Long> deletedAttachmentIds,
    @NotEmpty(message = "At least the default locale (en) translation must be provided")
        List<@Valid BlogTranslationRequest> translations) {

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
            .map(BlogTranslationRequest::locale)
            .distinct()
            .count();
    return distinctCount == translations.size();
  }
}
