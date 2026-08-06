package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchDeleteRequest(
    @NotEmpty(message = "At least one ID must be provided") List<@NotNull Long> ids) {}
