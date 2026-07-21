package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

public record BlogResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id,
    String slug,
    String title,
    String content,
    String featuredImage,
    int viewsCount,
    int likesCount,
    int dislikesCount,
    Boolean isPublished,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<BlogAttachmentResponse> blogAttachments) {}
