package com.api.rizz.portfolio_api.dto.request;

import java.time.LocalDate;

import com.api.rizz.portfolio_api.entity.User.AuthProvider;
import com.api.rizz.portfolio_api.entity.User.Gender;
import com.api.rizz.portfolio_api.entity.User.Role;

import jakarta.validation.constraints.NotBlank;

public record UserRequest(
    @NotBlank(message = "Nickname cannot be empty") String nickname,
    String fullName,
    @NotBlank(message = "Email cannot be empty") String email,
    String password,
    Role role,
    AuthProvider provider,
    String profilePictUrl,
    String placeOfBirth,
    LocalDate dateOfBirth,
    Gender gender,
    String phoneNumber,
    String bio,
    String address) {
}
