package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.security.jose.PublicKeyService;
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
@RequestMapping()
public class JwkController {

    private final PublicKeyService publicKeyService;

    @Autowired
    public JwkController(PublicKeyService publicKeyService) {
        this.publicKeyService = publicKeyService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwk() {
        List<Map<String, Object>> jwks = publicKeyService.getAllPublicJwks();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(Map.of("keys", jwks));
    }

    @GetMapping("/admin/jwks/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Map<String, Object>> getJwks() {
        return ApiResponse.success(Map.of("keys", List.of(publicKeyService.getAllActiveJwks())));
    }

    @GetMapping("/admin/jwks/active")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ApiResponse<Map<String, Object>> getActiveJwks() {
        return ApiResponse.success(Map.of("keys", List.of(publicKeyService.getJwk())));
    }
}