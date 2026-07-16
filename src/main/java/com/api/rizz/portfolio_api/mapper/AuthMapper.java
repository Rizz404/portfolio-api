package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.RegisterRequest;
import com.api.rizz.portfolio_api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {

  // * Abaikan field yang akan di-generate manual oleh sistem
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "provider", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "fullName", ignore = true)
  User toEntity(RegisterRequest request);
}
