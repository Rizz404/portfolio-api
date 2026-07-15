package com.api.rizz.portfolio_api.dto.response;

public record AuthResponse(
        String token,
        UserResponse user) {
}
