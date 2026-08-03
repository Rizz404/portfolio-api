package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record BlogRequest(
    @NotBlank(message = "Blog title cannot be empty") @Size(max = 255, message = "Blog title must not exceed 255 characters") String title,
    @NotBlank(message = "Blog content cannot be empty") String content,
    Boolean isPublished,
    @URL(message = "Featured image URL must be a valid URL") String featuredImageUrl,
    @PositiveOrZero(message = "Views count cannot be negative") int viewsCount,
    @PositiveOrZero(message = "Likes count cannot be negative") int likesCount,
    @PositiveOrZero(message = "Dislikes count cannot be negative") int dislikesCount,
    List<Long> deletedAttachmentIds) {}
