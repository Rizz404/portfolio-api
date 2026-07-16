package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
    @NotBlank String companyName,
    @NotBlank String position,
    String description,
    List<String> jobdesks,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent) {}
