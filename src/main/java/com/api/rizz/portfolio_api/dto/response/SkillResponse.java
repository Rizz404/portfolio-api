package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.Skill.SkillCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

public record SkillResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING) String id,
    String name,
    SkillCategory category,
    String logoUrl,
    String description,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
