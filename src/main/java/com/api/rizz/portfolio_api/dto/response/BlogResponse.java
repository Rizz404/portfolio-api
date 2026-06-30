package com.api.rizz.portfolio_api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import com.api.rizz.portfolio_api.entity.BlogAttachment;

public record BlogResponse(
    String id,
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
    List<BlogAttachment> blogAttachments) {
}
