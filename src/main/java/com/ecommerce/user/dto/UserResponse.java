package com.ecommerce.user.dto;

import com.ecommerce.user.entity.AccountStatus;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate dateOfBirth,
        String address,
        String profileImage,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant updatedAt) implements Serializable {
}
