package com.api.rizz.portfolio_api.dto.response;

import com.api.rizz.portfolio_api.entity.User.AuthProvider;
import com.api.rizz.portfolio_api.entity.User.Gender;
import com.api.rizz.portfolio_api.entity.User.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record UserResponse(@JsonFormat(shape = JsonFormat.Shape.STRING) String id, String nickname,
        String fullName, String email, String password, Role role, AuthProvider provider,
        String profilePict, String placeOfBirth, LocalDate dateOfBirth, Gender gender,
        String phoneNumber, String bio, String address, OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
