package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BlogAttachmentRequest(
                Long blogId,
                @NotBlank(message = "File name cannot be empty") String fileName,
                @NotBlank(message = "File url cannot be empty") String fileUrl,
                @NotBlank(message = "File type cannot be empty") String fileType) {
}
