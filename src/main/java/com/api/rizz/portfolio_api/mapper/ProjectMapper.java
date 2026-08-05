package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.Project;
import com.api.rizz.portfolio_api.entity.ProjectTranslation;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  // * translations dibangun manual di Service (butuh back-reference ke Project & locale
  // * fallback), bukan lewat MapStruct
  @Mapping(target = "translations", ignore = true)
  Project toEntity(ProjectRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * name/description/resolvedLocale di-resolve dari translations sesuai locale request
  // * (Accept-Language header), bukan dari kolom flat di entity (sudah dipindah ke tabel
  // * project_translations)
  @Mapping(target = "name", expression = "java(resolveTranslation(entity).getName())")
  @Mapping(target = "description", expression = "java(resolveTranslation(entity).getDescription())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  ProjectResponse toResponse(Project entity);

  // Mapper bakal otomatis ngisi data dari Request ke Entity lama yang udah ada
  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(ProjectRequest request, @MappingTarget Project entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default ProjectTranslation resolveTranslation(Project entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
