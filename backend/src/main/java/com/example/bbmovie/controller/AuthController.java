package com.example.bbmovie.controller;

import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.*;
import com.example.bbmovie.dto.response.AccessTokenResponse;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.LoginResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.exception.UnauthorizedUserException;
import com.example.bbmovie.service.auth.RefreshTokenService;
import com.example.bbmovie.service.auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<String>> hello() {
        return ResponseEntity.ok(ApiResponse.success("Hello World!"));
    }

    @GetMapping("/test")
    @PreAuthorize("isAccountOwner(#userName, #authentication)")
    public ResponseEntity<String> testSecurityExpression(String userName, @AuthenticationPrincipal UserDetails authentication) {
        return ResponseEntity.ok("Hello " + userName + "!" + authentication + " is authenticated.");
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        return ResponseEntity.ok(ApiResponse.success(authService.loadAuthenticatedUserInformation(userDetails.getUsername())));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult
    ) {
        ResponseEntity<ApiResponse<AuthResponse>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(
                response, "Registration successful. Please check your email for verification."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            BindingResult bindingResult
    ) {
        ResponseEntity<ApiResponse<LoginResponse>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;

        LoginResponse response = authService.login(loginRequest, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

//    @PostMapping("/login/ott")
//    public ResponseEntity<ApiResponse<LoginResponse>> loginWithOtt() {
//        LoginResponse response = authService.loginWithOtt();
//        return ResponseEntity.ok(ApiResponse.success(response));
//    }

    @PostMapping("/access-token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> getAccessToken(
            @RequestHeader(value = "X-DEVICE-NAME") String deviceName,
            @RequestHeader(value = "Authorization") String oldAccessTokenHeader
    ) {
        if (!oldAccessTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
        }
        String oldAccessToken = oldAccessTokenHeader.substring(7);
        String newAccessToken = refreshTokenService.refreshAccessToken(oldAccessToken, deviceName);
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(accessTokenResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization") String tokenHeader,
            @RequestHeader(value = "X-DEVICE-NAME") String deviceName,
            HttpServletResponse response
    ) {
        boolean isValidAuthorizationHeader = tokenHeader.startsWith("Bearer ");
        if (isValidAuthorizationHeader) {
            String accessToken = tokenHeader.substring(7);
            authService.revokeAccessTokenAndRefreshToken(accessToken, deviceName);
            authService.revokeCookies(response);
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
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        authService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email has been resent. Please check your email."));
    }

    //TODO: revoke access token, refresh token after success
    //      (no transaction unless exception occurs when changing password)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    //TODO: revoke access token, refresh token after success
    //      (no transaction unless exception occurs when changing password)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult
    ) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        authService.sendForgotPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions have been sent to your email"));
    }

    //TODO: revoke access token, refresh token after success
    //      (no transaction unless exception occurs when changing password)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult
    ) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        authService.resetPassword(token, request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }

    //TODO: implement forced logout (revoke all tokens when user request: a email or otp send(choice) -> revoke when valid)
    @PostMapping("/force-logout")
    public ResponseEntity<ApiResponse<Void>> forceLogout() {
        return ResponseEntity.ok(ApiResponse.success("Logout from all devices successful"));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.ok().build();
    }
}
