package com.api.rizz.portfolio_api.mapper;

import com.api.rizz.portfolio_api.dto.request.BlogAttachmentRequest;
import com.api.rizz.portfolio_api.dto.response.BlogAttachmentResponse;
import com.api.rizz.portfolio_api.entity.BlogAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

// * componentModel = "spring" bikin mapper ini jadi Bean yang bisa di-@Autowired
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BlogAttachmentMapper {

  // * Konversi dari DTO ke Entity (Saat nyimpen data)
  @Mapping(target = "blog.id", source = "blogId")
  BlogAttachment toEntity(BlogAttachmentRequest request);

  // * MapStruct otomatis ubah Long ID ke String ID karena nama variabelnya sama!
  @Mapping(target = "blogId", source = "blog.id")
  BlogAttachmentResponse toResponse(BlogAttachment entity);

  void updateEntityFromRequest(BlogAttachmentRequest request, @MappingTarget BlogAttachment entity);
}
