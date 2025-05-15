package com.example.bbmovie.controller;

import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.*;
import com.example.bbmovie.dto.response.*;
import com.example.bbmovie.exception.UnauthorizedUserException;
import com.example.bbmovie.service.auth.RefreshTokenService;
import com.example.bbmovie.service.auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

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
            authService.logoutFromCurrentDevice(accessToken, deviceName);
            authService.revokeCookies(response);
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyAccountByEmail(token)));
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

    @GetMapping("/sessions/devices")
    public ResponseEntity<ApiResponse<List<LoggedInDeviceResponse>>> getAllDeviceLoggedIntoAccount(
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request
    ) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        List<LoggedInDeviceResponse> devices = authService.getAllLoggedInDevices(userDetails.getUsername(), request);
        return devices.isEmpty()
                ? ResponseEntity.ok(ApiResponse.success(List.of()))
                : ResponseEntity.ok(ApiResponse.success(devices));
    }

    @PostMapping("/sessions/devices/revoke")
    public ResponseEntity<ApiResponse<String>> revokeDeviceLoggedIntoAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RevokeDeviceRequest request
    ) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        List<String> revokedDevices = new ArrayList<>();
        for (DeviceRevokeEntry device : request.getDevices()) {
            authService.logoutFromOneDevice(userDetails.getUsername(), device.getDeviceName());
            revokedDevices.add(device.getDeviceName());
        }
        return ResponseEntity.ok(ApiResponse.success(
                String.join(", ", revokedDevices) + " was forced to logout successfully")
        );
    }

    @GetMapping("/oauth2-callback")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUserFromOAuth2(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not authenticated"));
        }
        LoginResponse loginResponse = authService.getLoginResponseFromOAuth2Login(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @GetMapping("/user-agent")
    public ResponseEntity<ApiResponse<UserAgentResponse>> getUserAgent(HttpServletRequest request) {
        UserAgentResponse userAgentResponse = authService.getUserDeviceInformation(request);
        return ResponseEntity.ok(ApiResponse.success(userAgentResponse));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.ok().build();
    }
}
