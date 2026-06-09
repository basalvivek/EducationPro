package com.educationpro.auth.controller;

import com.educationpro.auth.dto.ForgotPasswordRequest;
import com.educationpro.auth.dto.LoginRequest;
import com.educationpro.auth.dto.LoginResponse;
import com.educationpro.auth.dto.ResetPasswordRequest;
import com.educationpro.auth.service.AuthService;
import com.educationpro.auth.service.PasswordResetService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                     HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(req);
        Cookie cookie = new Cookie("ep_jwt", loginResponse.token());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) loginResponse.expiresIn());
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("status", "success", "data", loginResponse));
    }

    @PostMapping("/forgot-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.sendResetLink(req);
        return ResponseEntity.ok(Map.of("message", "Reset link sent if account exists."));
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token,
                                    org.springframework.ui.Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        passwordResetService.resetPassword(req);
        return ResponseEntity.ok(Map.of("message", "Password updated. You may now log in."));
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("ep_jwt", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/auth/login";
    }
}
