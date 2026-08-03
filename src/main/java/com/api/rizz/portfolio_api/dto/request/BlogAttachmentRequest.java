package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.BlogAttachment.AttachmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record BlogAttachmentRequest(
    @NotNull(message = "Blog id must be filled") Long blogId,
    @NotBlank(message = "File name cannot be empty") @Size(max = 255, message = "File name must not exceed 255 characters") String fileName,
    @NotBlank(message = "File url cannot be empty") @URL(message = "File url must be a valid URL")
        String fileUrl,
    @NotBlank(message = "File type cannot be empty") @Size(max = 100, message = "File type must not exceed 100 characters") String fileType,
    @NotNull(message = "Attachment type must be provided") AttachmentType attachmentType) {}
