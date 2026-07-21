package com.api.rizz.portfolio_api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record ProjectResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id, // ID diubah
    // jadi string
    // biar
    // snowflakes
    // gak error
    String slug,
    String name,
    String description,
    String status,
    String logoUrl,
    List<String> imageUrls,
    Map<String, String> projectLinks,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
