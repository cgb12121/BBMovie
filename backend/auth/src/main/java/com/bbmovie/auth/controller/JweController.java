package com.bbmovie.auth.controller;

import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/jwe")
@RequiredArgsConstructor
public class JweController {

    private final JoseProviderStrategyContext jweService;

    @Value("${app.internal.api.key.auth}")
    private String apiKey;

    @PostMapping("/payload")
    public ResponseEntity<?> decodePayload(
            @RequestHeader("X-API-KEY") String clientApiKey,
            @RequestBody Map<String, String> body
    ) {
        if (!apiKey.equals(clientApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid API Key"));
        }

        String jweToken = body.get("token");
        try {
            Map<String, Object> payload = jweService.getActiveProvider().getClaimsFromToken(jweToken);
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
