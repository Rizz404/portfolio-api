package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.BlogAttachment.FileType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

public record BlogAttachmentResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id,
    String blogId, // Mengembalikan String ID, bukan objek Blog
    String fileName,
    String fileUrl,
    FileType fileType,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
