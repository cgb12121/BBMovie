package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.response.MfaSetupResult;
import com.bbmovie.auth.dto.response.MfaVerifyResponse;
import com.bbmovie.auth.dto.request.MfaVerifyRequest;
import com.bbmovie.auth.dto.response.MfaSetupResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.service.auth.mfa.MfaNotificationService;
import com.bbmovie.auth.service.auth.mfa.TotpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final TotpService totpService;
    private final MfaNotificationService mfaTotpProducer;
    private final UserRepository userRepository;

    @Autowired
    public MfaController(
            TotpService totpService,
            MfaNotificationService mfaTotpProducer,
            UserRepository userRepository
    ) {
        this.totpService = totpService;
        this.mfaTotpProducer = mfaTotpProducer;
        this.userRepository = userRepository;
    }

    /**
     * <b>WARNING:</b> This method is considered unsafe
     */
    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA already enabled");
        }

        MfaSetupResult result = totpService.generateSetupData(email);
        user.setTotpSecret(result.secret());
        user.setMfaEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(new MfaSetupResponse(result.secret(), result.qrCode()));
    }


    /**
     * <b>WARNING:</b> This method is considered unsafe
     */
    @PostMapping("/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(@RequestBody MfaVerifyRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.isMfaEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA not enabled");
        }

        boolean isValid = totpService.verifyCode(request.code(), user.getTotpSecret());
        if (!isValid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TOTP code");
        }

        mfaTotpProducer.sendRecoveryCode(
                String.valueOf(user.getId()),
                user.getTotpSecret(),
                user.getEmail(),
                user.getPhoneNumber()
        );

        return ResponseEntity.ok(new MfaVerifyResponse("MFA verified and recovery code sent"));
    }
}