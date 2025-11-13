package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.response.MfaSetupResult;
import com.bbmovie.auth.dto.response.MfaVerifyResponse;
import com.bbmovie.auth.dto.request.MfaVerifyRequest;
import com.bbmovie.auth.dto.response.MfaSetupResponse;
import com.bbmovie.auth.service.auth.mfa.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final MfaService mfaService;

    @Autowired
    public MfaController(MfaService mfaService) {
        this.mfaService = mfaService;
    }

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@RequestHeader("Authorization") String authorizationHeader) {
        MfaSetupResult result = mfaService.getMfaSetupResult(authorizationHeader);
        return ResponseEntity.ok(new MfaSetupResponse(result.secret(), result.qrCode()));
    }

    @PostMapping("/verify")
    public ResponseEntity<MfaVerifyResponse> verifyMfa(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody MfaVerifyRequest request
    ) {
        mfaService.verify(authorizationHeader, request);
        return ResponseEntity.ok(new MfaVerifyResponse("MFA enabled successfully"));
    }
}