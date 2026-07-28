package com.api.rizz.portfolio_api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @Email(message = "Format is not valid email") @NotBlank(message = "Email must be filled") String email,
    @NotBlank(message = "Password must be filled") @Size(min = 8, message = "Password must be at least 8 characters long") String password,
    @NotBlank(message = "Confirm Password must be filled") String confirmPassword,
    @NotBlank(message = "Nickname must be filled") @Size(max = 255, message = "Nickname must not exceed 255 characters") String nickname) {

  @AssertTrue(message = "Password and confirm password do not match") public boolean isPasswordConfirmed() {
    return password == null || password.equals(confirmPassword);
  }
}
