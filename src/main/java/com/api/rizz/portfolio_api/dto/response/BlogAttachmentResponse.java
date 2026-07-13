package com.api.rizz.portfolio_api.dto.response;

import java.time.OffsetDateTime;

public record BlogAttachmentResponse(
        String id,
        String blogId, // Mengembalikan String ID, bukan objek Blog
        String fileName,
        String fileUrl,
        String fileType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
