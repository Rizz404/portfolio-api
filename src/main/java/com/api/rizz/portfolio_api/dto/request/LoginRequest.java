package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email(message = "Format is not valid email") @NotBlank(message = "Email must be filled") String email,
    @NotBlank(message = "Password must be filled") String password) {
}
