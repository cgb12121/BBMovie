package com.bbmovie.auth.controller;

import com.bbmovie.auth.common.ValidationHandler;
import com.bbmovie.auth.controller.openapi.AuthControllerOpenApi;
import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.*;
import com.bbmovie.auth.dto.response.AccessTokenResponse;
import com.bbmovie.auth.dto.response.AuthResponse;
import com.bbmovie.auth.dto.response.LoginResponse;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.service.auth.RefreshTokenService;
import com.bbmovie.auth.service.auth.password.PasswordService;
import com.bbmovie.auth.service.auth.registration.RegistrationService;
import com.bbmovie.auth.service.auth.session.SessionService;
import com.bbmovie.auth.service.auth.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AuthController implements AuthControllerOpenApi {

    private final SessionService sessionService;
    private final RegistrationService registrationService;
    private final PasswordService passwordService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return new ResponseEntity<>("Hello World", HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<AuthResponse>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        registrationService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful. Please check your email for verification."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest hsr,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<LoginResponse>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        LoginResponse response = sessionService.login(loginRequest, hsr);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/abac/new-access-token")
    public ResponseEntity<String> getNewAccessTokenForLatestAbac(@RequestHeader(value = "Authorization") String oldAccessToken) {
        return ResponseEntity.ok(refreshTokenService.refreshAccessToken(oldAccessToken));
    }

    @PostMapping("/access-token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> getAccessTokenV1(
            @RequestHeader(value = "Authorization") String oldBearerToken) {
        String oldAccessToken = oldBearerToken;
        if (oldBearerToken.startsWith("Bearer ")) {
            oldAccessToken = oldBearerToken.substring(7);
        }
        return ResponseEntity.ok(ApiResponse.success(new AccessTokenResponse(refreshTokenService.refreshAccessToken(oldAccessToken))));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization") String tokenHeader,
            @AuthenticationPrincipal Authentication authentication,
            HttpServletResponse response) {
        boolean isValidAuthorizationHeader = tokenHeader.startsWith("Bearer ");
        if (isValidAuthorizationHeader) {
            String accessToken = tokenHeader.substring(7);
            sessionService.logoutFromCurrentDevice(accessToken);
            sessionService.revokeAuthCookies(response);
            SecurityContextHolder.clearContext();
            authentication.setAuthenticated(false);
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Invalid authorization header"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.verifyAccountByEmail(token)));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody SendVerificationEmailRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        registrationService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email has been resent. Please check your email."));
    }

    //TODO: change UserDetails => jwt (user must log in to change password)
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            BindingResult bindingResult,
            @AuthenticationPrincipal UserDetails userDetails) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        passwordService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        passwordService.sendForgotPasswordEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions have been sent to your email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody ResetPasswordRequest request,
            BindingResult bindingResult) {
        ResponseEntity<ApiResponse<Void>> errorResponse = ValidationHandler.handleBindingErrors(bindingResult);
        if (errorResponse != null) return errorResponse;
        passwordService.resetPassword(token, request);
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully"));
    }

    @GetMapping("/oauth2-callback")
    public ResponseEntity<ApiResponse<LoginResponse>> getCurrentUserFromOAuth2(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest hsr) {
        LoginResponse loginResponse = sessionService.getLoginResponseFromOAuth2Login(userDetails, hsr);
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @GetMapping("/user-agent")
    public ResponseEntity<ApiResponse<UserAgentResponse>> getUserAgent(HttpServletRequest hsr) {
        UserAgentResponse userAgentResponse = userService.getUserDeviceInformation(hsr);
        return ResponseEntity.ok(ApiResponse.success(userAgentResponse));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.ok().build();
    }
}
