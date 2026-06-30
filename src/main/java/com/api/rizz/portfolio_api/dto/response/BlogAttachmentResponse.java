package com.api.rizz.portfolio_api.dto.response;

import java.time.OffsetDateTime;

import com.api.rizz.portfolio_api.entity.Blog;

public record BlogAttachmentResponse(
    String id,
    Blog blog,
    String fileName,
    String fileUrl,
    String fileType,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {
}
