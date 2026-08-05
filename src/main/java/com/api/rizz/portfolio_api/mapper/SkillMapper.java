package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.SkillRequest;
import com.api.rizz.portfolio_api.dto.response.SkillResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Skill;
import com.api.rizz.portfolio_api.entity.SkillTranslation;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SkillMapper {

  // * translations dibangun manual di Service (butuh back-reference ke Skill & locale fallback)
  @Mapping(target = "translations", ignore = true)
  Skill toEntity(SkillRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * description/resolvedLocale di-resolve dari translations sesuai locale request
  @Mapping(target = "description", expression = "java(resolveTranslation(entity).getDescription())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  SkillResponse toResponse(Skill entity);

  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(SkillRequest request, @MappingTarget Skill entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default SkillTranslation resolveTranslation(Skill entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
