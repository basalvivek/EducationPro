package com.educationpro.auth.dto;

public record LoginResponse(
        String token,
        long expiresIn,
        String role,
        String name,
        String email,
        String redirectTo
) {}
