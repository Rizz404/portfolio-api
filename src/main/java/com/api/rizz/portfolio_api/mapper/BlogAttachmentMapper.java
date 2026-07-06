package com.api.rizz.portfolio_api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.request.BlogRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.entity.Blog;
import com.api.rizz.portfolio_api.entity.BlogAttachment;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BlogAttachmentMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  BlogAttachment toEntity(BlogAttachmentRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  BlogAttachmentResponse toResponse(BlogAttachment entity);

  void updateEntityFromRequest(BlogAttachmentRequest request, @MappingTarget BlogAttachment entity);

}
