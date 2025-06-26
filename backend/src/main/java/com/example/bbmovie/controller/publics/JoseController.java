package com.example.bbmovie.controller.publics;

import com.example.bbmovie.security.jose.jwk.JwkService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/.well-known/jwks.json")
public class JoseController {

    private final JwkService jwkService;

    @Autowired
    public JoseController(JwkService jwkService) {
        this.jwkService = jwkService;
    }

    @GetMapping("/all")
    public Map<String, Object> getJwks() {
        return Map.of("keys", List.of(jwkService.getAllActiveJwks()));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getJwk() {
        List<Map<String, Object>> jwks = Collections.singletonList(jwkService.getJwk());

        Map<String, Object> result = Map.of("keys", jwks);
        log.info("Returning jwk: {}", result);
        return ResponseEntity.ok(result);
    }
}