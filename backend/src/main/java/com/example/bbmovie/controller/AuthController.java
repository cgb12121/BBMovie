package com.example.bbmovie.controller;

import com.example.bbmovie.common.BlindResult;
import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.RefreshTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.service.intf.AuthService;
import com.example.bbmovie.service.intf.RegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<BlindResult<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(BlindResult.validationError(ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(BlindResult.success(
                response, 
                "Registration successful. Please check your email for verification."
            ));
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                BlindResult.error("Registration failed: " + e.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<BlindResult<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                BlindResult.validationError(ValidationHandler.processValidationErrors(bindingResult.getAllErrors()))
            );
        }
        return ResponseEntity.ok(BlindResult.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BlindResult<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                BlindResult.validationError(ValidationHandler.processValidationErrors(bindingResult.getAllErrors()))
            );
        }
        return ResponseEntity.ok(BlindResult.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<BlindResult<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                BlindResult.validationError(ValidationHandler.processValidationErrors(bindingResult.getAllErrors()))
            );
        }
        authService.logout(request);
        return ResponseEntity.ok(BlindResult.success(null, "Logged out successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<BlindResult<Void>> verifyEmail(@RequestParam String token) {
        try {
            registrationService.verifyEmail(token);
            return ResponseEntity.ok(BlindResult.success(null, "Email verified successfully. You can now login."));
        } catch (RuntimeException e) {
            log.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BlindResult.error("Email verification failed: " + e.getMessage()));
        }
    }

    @PostMapping("/send-verification")
    public ResponseEntity<BlindResult<Void>> resendVerificationEmail(@RequestParam String email) {
        try {
            registrationService.sendVerificationEmail(email);
            return ResponseEntity.ok(BlindResult.success(null, "Verification email has been resent. Please check your email."));
        } catch (RuntimeException e) {
            log.error("Failed to resend verification email: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(BlindResult.error("Failed to resend verification email: " + e.getMessage()));
        }
    }
} 