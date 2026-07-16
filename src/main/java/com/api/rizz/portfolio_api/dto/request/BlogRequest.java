package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record BlogRequest(
    @NotBlank(message = "Blog title cannot be empty") String title,
    String content,
    String featuredImageUrl,
    Boolean isPublished,
    int viewsCount,
    int likesCount,
    int dislikesCount,
    List<Long> deletedAttachmentIds) {}
