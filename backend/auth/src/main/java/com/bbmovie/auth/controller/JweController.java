package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/jwe")
public class JweController {

    private final JoseProviderStrategyContext jweService;

    @Autowired
    public JweController(JoseProviderStrategyContext jweService) {
        this.jweService = jweService;
    }

    @Value("${app.internal.api.key.auth}")
    private String apiKey;

    @PostMapping("/payload")
    public ResponseEntity<?> decodePayload(
            @RequestHeader("X-API-KEY") String clientApiKey,
            @RequestBody Map<String, String> body
    ) {
        if (!apiKey.equals(clientApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid api key"));
        }
        String jweToken = body.get("token");
        Map<String, Object> payload = jweService.getActiveProvider().getClaimsFromToken(jweToken);
        return ResponseEntity.ok(payload);
    }
}
