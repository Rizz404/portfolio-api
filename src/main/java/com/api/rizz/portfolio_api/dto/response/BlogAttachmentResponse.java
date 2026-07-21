package com.api.rizz.portfolio_api.dto.response;

import java.time.OffsetDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

public record BlogAttachmentResponse(@JsonFormat(shape = JsonFormat.Shape.STRING) String id,
        String blogId, // Mengembalikan String ID, bukan objek Blog
        String fileName, String fileUrl, String fileType, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
