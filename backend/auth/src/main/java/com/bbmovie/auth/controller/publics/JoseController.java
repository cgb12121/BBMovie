package com.bbmovie.auth.controller.publics;

import com.bbmovie.auth.security.jose.config.JwkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/.well-known/jwks.json")
public class JoseController {

    private final JwkService jwkService;

    @Autowired
    public JoseController(JwkService jwkService) {
        this.jwkService = jwkService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getJwk() {
        List<Map<String, Object>> jwks = jwkService.getAllPublicJwks();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(Map.of("keys", jwks));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Map<String, Object> getJwks() {
        return Map.of("keys", List.of(jwkService.getAllActiveJwks()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Map<String, Object> getActiveJwks() {
        return Map.of("keys", List.of(jwkService.getJwk()));
    }
}