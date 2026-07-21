package com.api.rizz.portfolio_api.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;

public record ExperienceResponse(@JsonFormat(shape = JsonFormat.Shape.STRING) String id,
        String companyName, String position, String description, List<String> jobdesks,
        LocalDate startDate, LocalDate endDate, Boolean isCurrent, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
