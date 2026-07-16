package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.Use.Category;
import java.time.OffsetDateTime;
import java.util.List;

public record UseResponse(
    String id,
    String itemName,
    Category category,
    String logoUrl,
    List<String> pictures,
    String reasons,
    List<String> links,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
