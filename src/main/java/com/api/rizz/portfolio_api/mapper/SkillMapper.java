package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.SkillRequest;
import com.api.rizz.portfolio_api.dto.response.SkillResponse;
import com.api.rizz.portfolio_api.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SkillMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  Skill toEntity(SkillRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  SkillResponse toResponse(Skill entity);

  void updateEntityFromRequest(SkillRequest request, @MappingTarget Skill entity);
}
