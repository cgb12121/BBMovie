package com.bbmovie.auth.controller;

import com.bbmovie.auth.common.ValidationHandler;
import com.bbmovie.auth.controller.openapi.AuthControllerOpenApi;
import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.*;
import com.bbmovie.auth.dto.response.AccessTokenResponse;
import com.bbmovie.auth.dto.response.AuthResponse;
import com.bbmovie.auth.dto.response.LoginResponse;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.service.auth.AuthService;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController implements AuthControllerOpenApi {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return new ResponseEntity<>("Hello World", HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult
    ) {
        ResponseEntity<ApiResponse<AuthResponse>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please check your email for verification."));
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


    @GetMapping("/abac/new-access-token")
    public ResponseEntity<String> getNewAccessTokenForLatestAbac(@RequestHeader(value = "Authorization") String oldAccessToken) {
        return ResponseEntity.ok(refreshTokenService.refreshAccessToken(oldAccessToken));
    }

    @PostMapping("/v2/access-token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> getAccessTokenV1(
            @RequestHeader(value = "Authorization") String oldAccessTokenHeader
    ) {
        if (!oldAccessTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
        }
        String oldAccessToken = oldAccessTokenHeader.substring(7);
        String newAccessToken = refreshTokenService.refreshAccessToken(oldAccessToken);
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse(newAccessToken);
        return ResponseEntity.ok(ApiResponse.success(accessTokenResponse));
    }

    @PostMapping("/v2/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization") String tokenHeader,
            @AuthenticationPrincipal Authentication authentication,
            HttpServletResponse response
    ) {
        boolean isValidAuthorizationHeader = tokenHeader.startsWith("Bearer ");
        if (isValidAuthorizationHeader) {
            String accessToken = tokenHeader.substring(7);
            authService.logoutFromCurrentDevice(accessToken);
            authService.revokeAuthCookies(response);
            SecurityContextHolder.clearContext();
            authentication.setAuthenticated(false);
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

    @GetMapping("/oauth2-callback")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUserFromOAuth2(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
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
