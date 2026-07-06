package com.api.rizz.portfolio_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.api.rizz.portfolio_api.dto.request.ProjectRequest;
import com.api.rizz.portfolio_api.dto.response.ProjectResponse;
import com.api.rizz.portfolio_api.entity.Project;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  Project toEntity(ProjectRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  ProjectResponse toResponse(Project entity);

  // Mapper bakal otomatis ngisi data dari Request ke Entity lama yang udah ada
  void updateEntityFromRequest(ProjectRequest request, @MappingTarget Project entity);
}
