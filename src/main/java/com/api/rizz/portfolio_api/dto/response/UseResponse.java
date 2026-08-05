package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.Use.Category;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;

public record UseResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id,
    String itemName,
    String reasons,
    String resolvedLocale,
    Category category,
    String logoUrl,
    List<String> pictures,
    List<String> links,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
