package com.educationpro.auth.service;

import com.educationpro.auth.dto.LoginRequest;
import com.educationpro.auth.dto.LoginResponse;
import com.educationpro.domain.Role;
import com.educationpro.domain.User;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.UserRepository;
import com.educationpro.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest req) {
        Role role;
        try {
            role = Role.valueOf(req.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role: " + req.role());
        }

        User user = userRepository.findByEmailAndRole(req.email(), role)
                .orElseThrow(() -> {
                    log.warn("Failed login attempt for email={} role={}", req.email(), role);
                    return new BusinessException("Invalid credentials or role mismatch.");
                });

        if (!user.isActive()) {
            log.warn("Inactive user login attempt email={}", req.email());
            throw new BusinessException("Account is inactive.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            log.warn("Bad password for email={} role={}", req.email(), role);
            throw new BusinessException("Invalid credentials or role mismatch.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), role.name());
        log.info("Successful login email={} role={}", user.getEmail(), role);

        return new LoginResponse(
                token,
                expirationMs / 1000,
                role.name(),
                user.getFullName(),
                user.getEmail(),
                redirectFor(role)
        );
    }

    private String redirectFor(Role role) {
        return switch (role) {
            case ADMIN   -> "/admin/dashboard";
            case TEACHER -> "/teacher/dashboard";
            case STUDENT -> "/student/dashboard";
            case PARENT  -> "/parent/dashboard";
        };
    }
}
