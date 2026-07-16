package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UseRequest(
    @NotBlank(message = "Item name cannot be empty") String itemName,
    @NotBlank(message = "Category be empty") String category,
    String logoUrl,
    List<String> pictures,
    String reasons,
    List<String> links,
    List<String> deletedPictures) {}
