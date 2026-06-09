package com.educationpro.auth.service;

import com.educationpro.auth.dto.ForgotPasswordRequest;
import com.educationpro.auth.dto.ResetPasswordRequest;
import com.educationpro.domain.PasswordResetToken;
import com.educationpro.domain.Role;
import com.educationpro.exception.BusinessException;
import com.educationpro.repository.PasswordResetTokenRepository;
import com.educationpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public void sendResetLink(ForgotPasswordRequest req) {
        Role role;
        try {
            role = Role.valueOf(req.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            return; // silently ignore invalid role — no enumeration
        }

        userRepository.findByEmailAndRole(req.email(), role).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken prt = new PasswordResetToken();
            prt.setUser(user);
            prt.setToken(token);
            prt.setExpiresAt(Instant.now().plus(TOKEN_TTL));
            tokenRepository.save(prt);
            mailService.sendResetEmail(user.getEmail(), token);
            log.info("Password reset requested for email={} role={}", req.email(), role);
        });
        // Always return without error — no enumeration leak
    }

    public void resetPassword(ResetPasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new BusinessException("Passwords do not match.");
        }

        PasswordResetToken prt = tokenRepository.findByToken(req.token())
                .orElseThrow(() -> new BusinessException("Token expired or already used."));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expired or already used.");
        }

        prt.getUser().setPasswordHash(passwordEncoder.encode(req.newPassword()));
        prt.setUsed(true);
        log.info("Password reset completed for user id={}", prt.getUser().getId());
    }
}
