package com.example.bbmovie.controller;

import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.*;
import com.example.bbmovie.dto.response.AccessTokenResponse;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.exception.UnauthorizedUserException;
import com.example.bbmovie.service.OAuth2Service;
import com.example.bbmovie.service.RefreshTokenService;
import com.example.bbmovie.service.intf.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    private final OAuth2Service OAuth2Service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) { throw new UnauthorizedUserException("User not authenticated"); }
        return ResponseEntity.ok(ApiResponse.success(authService.loadAuthenticatedUser(userDetails.getUsername())));
    }

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
        authService.verifyEmail(token);
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

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> oauth2Callback(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam("state") String state
    ) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null || name == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Required user information is missing from Google OAuth2 response"));
        }
        AuthResponse response = OAuth2Service.loginViaGoogle(email, name);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/do-something-with-google-issued-token")
    public String doSomethingWithGoogleTokens(
        @AuthenticationPrincipal OAuth2User principal,
        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client
    ) {
        OAuth2AuthorizedClient googleAccount = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "registrationId/clientId",
                principal.getName()
        );

        String googleAccessToken = googleAccount.getAccessToken().getTokenValue();
        if (googleAccount.getRefreshToken() != null) {
            String googleRefreshToken = googleAccount.getRefreshToken().getTokenValue();
        }

        Authentication googleUser = new UsernamePasswordAuthenticationToken(client, null);

        oAuth2AuthorizedClientService.saveAuthorizedClient(client, googleUser);
        return null;
    }
    

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test-cache-control")
    public ResponseEntity<String> getUser() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                .body("hello");
    }
}
