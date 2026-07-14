package com.api.rizz.portfolio_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.request.UserRequest;
import com.api.rizz.portfolio_api.dto.response.UserResponse;
import com.api.rizz.portfolio_api.entity.User;
import com.api.rizz.portfolio_api.entity.User;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  User toEntity(UserRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  UserResponse toResponse(User entity);

  void updateEntityFromRequest(UserRequest request, @MappingTarget User entity);

}
