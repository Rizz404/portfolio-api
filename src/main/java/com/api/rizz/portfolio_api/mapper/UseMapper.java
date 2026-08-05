package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Use;
import com.api.rizz.portfolio_api.entity.UseTranslation;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UseMapper {

  // * translations dibangun manual di Service (butuh back-reference ke Use & locale fallback)
  @Mapping(target = "translations", ignore = true)
  Use toEntity(UseRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * reasons/resolvedLocale di-resolve dari translations sesuai locale request
  @Mapping(target = "reasons", expression = "java(resolveTranslation(entity).getReasons())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  UseResponse toResponse(Use entity);

  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(UseRequest request, @MappingTarget Use entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default UseTranslation resolveTranslation(Use entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
