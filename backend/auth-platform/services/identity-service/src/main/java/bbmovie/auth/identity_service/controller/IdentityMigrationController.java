package bbmovie.auth.identity_service.controller;

import bbmovie.auth.identity_service.dto.ChangePasswordRequest;
import bbmovie.auth.identity_service.dto.ForgotPasswordRequest;
import bbmovie.auth.identity_service.dto.RegisterRequest;
import bbmovie.auth.identity_service.dto.ResetPasswordRequest;
import bbmovie.auth.identity_service.dto.SendVerificationEmailRequest;
import bbmovie.auth.identity_service.dto.VerifyCredentialsRequest;
import bbmovie.auth.identity_service.config.CutoverProperties;
import bbmovie.auth.identity_service.service.IdentityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class IdentityMigrationController {
    private final IdentityService identityService;
    private final CutoverProperties cutover;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest body) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        String token = identityService.register(body);
        return ResponseEntity.ok(Map.of("success", true, "message", "Registration successful. Please verify email.", "verificationToken", token));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("token") String token) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        identityService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account verification successful"));
    }

    @PostMapping("/send-verification")
    public ResponseEntity<Map<String, Object>> resendVerification(@Valid @RequestBody SendVerificationEmailRequest body) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        String token = identityService.sendVerification(body);
        return ResponseEntity.ok(Map.of("success", true, "message", "Verification email request accepted", "verificationToken", token));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader(value = "X-User-Email", required = false) String principalEmail,
            @Valid @RequestBody ChangePasswordRequest body
    ) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        if (principalEmail == null || principalEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "X-User-Email header is required"));
        }
        identityService.changePassword(principalEmail, body);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        String token = identityService.forgotPassword(body);
        return ResponseEntity.ok(Map.of("success", true, "message", "If account exists, reset instructions were generated", "resetToken", token));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestParam("token") String token,
            @Valid @RequestBody ResetPasswordRequest body
    ) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        identityService.resetPassword(token, body);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully"));
    }

    @PostMapping("/internal/users/verify-credentials")
    public ResponseEntity<Map<String, Object>> verifyCredentials(@Valid @RequestBody VerifyCredentialsRequest body) {
        if (!cutover.isEnabled()) return cutoverDisabled();
        var verified = identityService.verifyCredentials(body);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", verified.userId(),
                "email", verified.email(),
                "role", verified.role(),
                "enabled", verified.enabled(),
                "isMfaEnabled", verified.mfaEnabled()
        ));
    }

    private ResponseEntity<Map<String, Object>> cutoverDisabled() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", "Identity cutover disabled. Route this endpoint to legacy auth-service."
        ));
    }
}
