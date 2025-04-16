package com.example.bbmovie.controller;

import com.example.bbmovie.common.ValidationHandler;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.*;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.model.User;
import com.example.bbmovie.service.intf.AuthService;
import com.example.bbmovie.service.intf.RegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationService registrationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success((User) userDetails));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Registration successful. Please check your email for verification."
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody AccessTokenRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AccessTokenRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam("token") String token) {
        registrationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now login."));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
            @Valid @RequestBody SendVerificationEmailRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.validationError(
                            ValidationHandler.processValidationErrors(bindingResult.getAllErrors())));
        }

        registrationService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Verification email has been resent. Please check your email."));
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> getCsrfToken() {
        return ResponseEntity.ok().build();
    }

    // @GetMapping("/csrf")
    // public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
    //     When we make a request to /auth/csrf, the actual CSRF token is automatically included in the response as a cookie named 'XSRF-TOKEN' by Spring Security's CookieCsrfTokenRepository. 
    //     We don't need to manually return the token in the response body.
    //     CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    
    //     return ResponseEntity.ok(Map.of(
    //         "token", csrf.getToken(),
    //         "headerName", csrf.getHeaderName()
    //     ));
    // }
}
