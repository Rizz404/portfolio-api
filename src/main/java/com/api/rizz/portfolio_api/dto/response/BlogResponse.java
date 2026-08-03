package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

public record BlogResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id,
    String slug,
    String title,
    String content,
    Boolean isPublished,
    String featuredImage,
    int viewsCount,
    int likesCount,
    int dislikesCount,
    List<BlogAttachmentResponse> blogAttachments,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
