package com.example.bbmovie.controller;

import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.*;
import com.example.bbmovie.dto.response.AccessTokenResponse;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UnauthorizedUserException;
import com.example.bbmovie.service.UserService;
import com.example.bbmovie.service.auth.RefreshTokenService;
import com.example.bbmovie.service.auth.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(
                response, "Registration successful. Please check your email for verification."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/access-token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> accessTokenFromRefreshToken(
            @Valid @RequestBody AccessTokenRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        String newAccessToken = refreshTokenService.refreshAccessToken(request.getRefreshToken());
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(accessTokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String tokenHeader) {
        boolean isValidAuthorizationHeader = tokenHeader.startsWith("Bear") || tokenHeader.startsWith("Authorization");
        if (isValidAuthorizationHeader) {
            String accessToken = tokenHeader.substring(7);
            authService.logout(accessToken);
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyAccountByEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now login."));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody SendVerificationEmailRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        authService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email has been resent. Please check your email."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                    ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                    ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        authService.sendForgotPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions have been sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                    ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }
        authService.resetPassword(token, request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.ok().build();
    }
}
