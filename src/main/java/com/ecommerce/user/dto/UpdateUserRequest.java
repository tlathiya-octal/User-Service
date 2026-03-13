package com.ecommerce.user.dto;

import com.ecommerce.user.entity.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateUserRequest(
        @Email @Size(max = 255) String email,
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Pattern(regexp = "^[0-9+\\-()\\s]{7,20}$", message = "phoneNumber must be a valid phone number")
        String phoneNumber,
        @Past(message = "dateOfBirth must be in the past") LocalDate dateOfBirth,
        @Size(max = 500) String address,
        @Size(max = 1024) String profileImage,
        AccountStatus accountStatus) {
}
