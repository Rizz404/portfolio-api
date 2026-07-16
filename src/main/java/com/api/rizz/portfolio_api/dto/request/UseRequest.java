package com.api.rizz.portfolio_api.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record UseRequest(
                @NotBlank(message = "Item name cannot be empty") String itemName,
                @NotBlank(message = "Category be empty") String category,
                String logoUrl,
                List<String> pictures,
                String reasons,
                List<String> links,
                List<String> deletedPictures) {
}
