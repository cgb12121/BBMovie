package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.response.MfaSetupResult;
import com.bbmovie.auth.dto.response.MfaVerifyResponse;
import com.bbmovie.auth.dto.request.MfaVerifyRequest;
import com.bbmovie.auth.dto.response.MfaSetupResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import com.bbmovie.auth.service.auth.mfa.TotpService;
import com.bbmovie.auth.service.auth.verify.otp.OtpService;
import com.bbmovie.auth.service.kafka.TotpProducer;
import com.example.common.enums.NotificationType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final TotpService totpService;
    private final UserRepository userRepository;
    private final JoseProviderStrategyContext joseContext;
    private final OtpService otpService;
    private final TotpProducer totpProducer;

    public MfaController(
            TotpService totpService,
            UserRepository userRepository,
            JoseProviderStrategyContext joseContext,
            OtpService otpService,
            TotpProducer totpProducer
    ) {
        this.totpService = totpService;
        this.userRepository = userRepository;
        this.joseContext = joseContext;
        this.otpService = otpService;
        this.totpProducer = totpProducer;
    }

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@RequestHeader("Authorization") String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        Map<String, Object> claims = joseContext.getActiveProvider().getClaimsFromToken(token);
        String email = String.valueOf(claims.get(SUB));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (Boolean.TRUE.equals(user.isMfaEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA already enabled");
        }

        MfaSetupResult result = totpService.generateSetupData(email);
        user.setTotpSecret(result.secret());
        // Do NOT enable MFA yet; require OTP confirmation first
        user.setMfaEnabled(false);
        userRepository.save(user);

        // Generate one-time OTP and send via Kafka to email service
        String otp = otpService.generateOtpToken(user);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            totpProducer.sendNotification(String.valueOf(user.getId()), NotificationType.EMAIL, user.getEmail(),
                    "Your MFA setup confirmation code is: " + otp);
        }

        return ResponseEntity.ok(new MfaSetupResponse(result.secret(), result.qrCode()));
    }

    @PostMapping("/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody MfaVerifyRequest request
    ) {
        String token = extractBearerToken(authorizationHeader);
        Map<String, Object> claims = joseContext.getActiveProvider().getClaimsFromToken(token);
        String emailFromToken = String.valueOf(claims.get(SUB));

        // Validate OTP from cache and bind to the same email
        String emailFromOtp = otpService.getEmailForOtpToken(request.code());
        if (emailFromOtp == null || !emailFromToken.equals(emailFromOtp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(emailFromToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getTotpSecret() == null || user.getTotpSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA not initialized for this user");
        }

        // Invalidate OTP once used
        otpService.deleteOtpToken(request.code());

        // Enable MFA only after OTP confirmation
        user.setMfaEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(new MfaVerifyResponse("MFA enabled successfully"));
    }

    private String extractBearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid authorization header");
        }
        return header.substring(7);
    }
}