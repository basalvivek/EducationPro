package com.educationpro.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).+$",
            message = "Password must contain upper, lower, digit and special character."
        )
        String newPassword,

        @NotBlank String confirmPassword
) {}
