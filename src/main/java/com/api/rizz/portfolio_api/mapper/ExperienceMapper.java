package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.ExperienceRequest;
import com.api.rizz.portfolio_api.dto.response.ExperienceResponse;
import com.api.rizz.portfolio_api.entity.Experience;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExperienceMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  Experience toEntity(ExperienceRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  ExperienceResponse toResponse(Experience entity);

  void updateEntityFromRequest(ExperienceRequest request, @MappingTarget Experience entity);
}
