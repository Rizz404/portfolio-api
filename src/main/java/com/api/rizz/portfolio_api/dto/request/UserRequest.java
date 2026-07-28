package com.api.rizz.portfolio_api.dto.request;

import com.api.rizz.portfolio_api.entity.User.AuthProvider;
import com.api.rizz.portfolio_api.entity.User.Gender;
import com.api.rizz.portfolio_api.entity.User.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.hibernate.validator.constraints.URL;

public record UserRequest(
    @NotBlank(message = "Nickname cannot be empty") @Size(max = 255, message = "Nickname must not exceed 255 characters") String nickname,
    @Size(max = 255, message = "Full name must not exceed 255 characters") String fullName,
    @NotBlank(message = "Email cannot be empty") @Email(message = "Format is not valid email") String email,
    @Size(min = 8, message = "Password must be at least 8 characters long") String password,
    Role role,
    AuthProvider provider,
    @URL(message = "Profile picture URL must be a valid URL") String profilePictUrl,
    String placeOfBirth,
    @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
    Gender gender,
    @Pattern(regexp = "^[+]?[0-9\\-\\s()]{7,20}$", message = "Phone number format is not valid")
        String phoneNumber,
    String bio,
    String address) {}
