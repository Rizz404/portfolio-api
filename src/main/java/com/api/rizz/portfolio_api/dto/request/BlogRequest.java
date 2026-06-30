package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BlogRequest(
        @NotBlank(message = "Blog title cannot be empty") String title,
        String content,
        String featuredImage,
        Boolean isPublished,
        int viewsCount,
        int likesCount,
        int dislikesCount) {
}
