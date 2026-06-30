package com.api.rizz.portfolio_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.api.rizz.portfolio_api.dto.request.UseRequest;
import com.api.rizz.portfolio_api.dto.response.UseResponse;
import com.api.rizz.portfolio_api.entity.Use;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UseMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  Use toEntity(UseRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  UseResponse toResponse(Use entity);
}
