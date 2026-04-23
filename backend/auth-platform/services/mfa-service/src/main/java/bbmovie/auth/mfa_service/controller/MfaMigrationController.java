package bbmovie.auth.mfa_service.controller;

import bbmovie.auth.mfa_service.dto.request.GenerateOtpRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyMfaRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyOtpRequest;
import bbmovie.auth.mfa_service.dto.request.VerifyTotpRequest;
import bbmovie.auth.mfa_service.dto.response.MfaSetupResponse;
import bbmovie.auth.mfa_service.dto.response.MessageResponse;
import bbmovie.auth.mfa_service.dto.response.OtpResponse;
import bbmovie.auth.mfa_service.dto.response.VerifyOtpResponse;
import bbmovie.auth.mfa_service.dto.response.VerifyTotpResponse;
import bbmovie.auth.mfa_service.config.CutoverProperties;
import bbmovie.auth.mfa_service.service.MfaMigrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
public class MfaMigrationController {

    private final MfaMigrationService mfaMigrationService;
    private final CutoverProperties cutover;

    public MfaMigrationController(MfaMigrationService mfaMigrationService, CutoverProperties cutover) {
        this.mfaMigrationService = mfaMigrationService;
        this.cutover = cutover;
    }

    @PostMapping("/api/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setup(@RequestHeader("Authorization") String authorization) {
        if (!cutover.isEnabled()) return disabledMfaSetup();
        return ResponseEntity.ok(mfaMigrationService.setup(authorization));
    }

    @PostMapping("/api/mfa/verify")
    public ResponseEntity<MessageResponse> verify(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody VerifyMfaRequest body
    ) {
        if (!cutover.isEnabled()) return disabledMessage();
        mfaMigrationService.verify(authorization, body);
        return ResponseEntity.ok(new MessageResponse(true, "MFA enabled successfully"));
    }

    @PostMapping("/internal/mfa/verify-totp")
    public ResponseEntity<VerifyTotpResponse> verifyTotp(@Valid @RequestBody VerifyTotpRequest body) {
        if (!cutover.isEnabled()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerifyTotpResponse(false));
        boolean valid = mfaMigrationService.verifyTotp(body);
        return ResponseEntity.ok(new VerifyTotpResponse(valid));
    }

    @PostMapping("/internal/mfa/generate-otp")
    public ResponseEntity<OtpResponse> generateOtp(@Valid @RequestBody GenerateOtpRequest body) {
        if (!cutover.isEnabled()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new OtpResponse(false, null));
        String otp = mfaMigrationService.generateOtp(body);
        return ResponseEntity.ok(new OtpResponse(true, otp));
    }

    @PostMapping("/internal/mfa/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest body) {
        if (!cutover.isEnabled()) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new VerifyOtpResponse(false));
        boolean valid = mfaMigrationService.verifyOtp(body);
        return ResponseEntity.ok(new VerifyOtpResponse(valid));
    }

    private ResponseEntity<MfaSetupResponse> disabledMfaSetup() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new MfaSetupResponse(false, null, null));
    }

    private ResponseEntity<MessageResponse> disabledMessage() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new MessageResponse(false, "MFA cutover disabled. Route to legacy auth-service."));
    }
}
