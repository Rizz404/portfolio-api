package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchProjectRequest(
    @NotEmpty(message = "At least one project must be provided") @Size(max = 50, message = "A batch can contain at most 50 projects") List<@NotNull(message = "Project must not be null") @Valid ProjectRequest> projects) {}
