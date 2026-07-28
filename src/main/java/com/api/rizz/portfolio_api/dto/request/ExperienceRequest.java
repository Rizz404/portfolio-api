package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ExperienceRequest(
    @NotBlank(message = "Company name cannot be empty") @Size(max = 255, message = "Company name must not exceed 255 characters") String companyName,
    @NotBlank(message = "Position cannot be empty") @Size(max = 255, message = "Position must not exceed 255 characters") String position,
    String description,
    List<String> jobdesks,
    @NotNull(message = "Start date must be filled") @PastOrPresent(message = "Start date cannot be in the future") LocalDate startDate,
    LocalDate endDate,
    Boolean isCurrent) {

  @AssertTrue(message = "End date cannot be before start date") public boolean isEndDateValid() {
    return endDate == null || startDate == null || !endDate.isBefore(startDate);
  }

  @AssertTrue(message = "End date must be empty when the experience is marked as current") public boolean isCurrentExperienceValid() {
    return !Boolean.TRUE.equals(isCurrent) || endDate == null;
  }
}
