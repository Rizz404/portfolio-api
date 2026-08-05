package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.entity.Experience;
import com.api.rizz.portfolio_api.entity.ExperienceTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExperienceMapper {

  // * translations dibangun manual di Service (butuh back-reference ke Experience & locale
  // * fallback)
  @Mapping(target = "translations", ignore = true)
  Experience toEntity(ExperienceRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * position/description/jobdesks/resolvedLocale di-resolve dari translations sesuai locale
  // * request
  @Mapping(target = "position", expression = "java(resolveTranslation(entity).getPosition())")
  @Mapping(target = "description", expression = "java(resolveTranslation(entity).getDescription())")
  @Mapping(target = "jobdesks", expression = "java(resolveTranslation(entity).getJobdesks())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  ExperienceResponse toResponse(Experience entity);

  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(ExperienceRequest request, @MappingTarget Experience entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default ExperienceTranslation resolveTranslation(Experience entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
