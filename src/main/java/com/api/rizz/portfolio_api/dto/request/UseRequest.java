package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record UseRequest(
    @NotBlank(message = "Item name cannot be empty") @Size(max = 255, message = "Item name must not exceed 255 characters") String itemName,
    @NotBlank(message = "Category cannot be empty") @Pattern(
            regexp = "software|hardware",
            message = "Category must be either 'software' or 'hardware'")
        String category,
    @URL(message = "Logo URL must be a valid URL") String logoUrl,
    List<String> pictures,
    String reasons,
    List<String> links,
    List<String> deletedPictures) {}
