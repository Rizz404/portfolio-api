package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.response.UserResponse;
import com.api.rizz.portfolio_api.entity.LanguageCode;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.UserTranslation;
import com.api.rizz.portfolio_api.util.TranslationResolver;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  // * translations dibangun manual di Service (butuh back-reference ke User & locale fallback)
  @Mapping(target = "translations", ignore = true)
  User toEntity(UserRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  // * bio/resolvedLocale di-resolve dari translations sesuai locale request
  @Mapping(target = "bio", expression = "java(resolveTranslation(entity).getBio())")
  @Mapping(
      target = "resolvedLocale",
      expression = "java(resolveTranslation(entity).getLocale().name())")
  UserResponse toResponse(User entity);

  @Mapping(target = "translations", ignore = true)
  void updateEntityFromRequest(UserRequest request, @MappingTarget User entity);

  default LanguageCode currentLocale() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    try {
      return LanguageCode.valueOf(lang);
    } catch (IllegalArgumentException e) {
      return LanguageCode.en;
    }
  }

  default UserTranslation resolveTranslation(User entity) {
    return TranslationResolver.resolve(entity.getTranslations(), currentLocale(), LanguageCode.en);
  }
}
