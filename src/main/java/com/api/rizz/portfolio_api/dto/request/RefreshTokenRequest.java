package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token must be filled") String refreshToken) {}
