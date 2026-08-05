package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogTranslation;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BlogMapper {

  // * translations dibangun manual di Service (butuh back-reference ke Blog & locale fallback)
  @Mapping(target = "translations", ignore = true)
  Blog toEntity(BlogRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * title/content/resolvedLocale di-resolve dari translations sesuai locale request
  @Mapping(target = "title", expression = "java(resolveTranslation(entity).getTitle())")
  @Mapping(target = "content", expression = "java(resolveTranslation(entity).getContent())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  BlogResponse toResponse(Blog entity);

  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(BlogRequest request, @MappingTarget Blog entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default BlogTranslation resolveTranslation(Blog entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
