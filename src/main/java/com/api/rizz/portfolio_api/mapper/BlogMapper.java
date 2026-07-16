package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BlogMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  Blog toEntity(BlogRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  BlogResponse toResponse(Blog entity);

  void updateEntityFromRequest(BlogRequest request, @MappingTarget Blog entity);
}
