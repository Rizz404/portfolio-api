package com.api.rizz.portfolio_api.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExperienceRequest(
        @NotBlank String companyName,
        @NotBlank String position,
        String description,
        List<String> jobdesks,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent) {
}
