package com.example.bbmovie.controller;

import com.example.bbmovie.security.jose.jwk.JwkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known/jwks.json")
public class JwksController {

    private final JwkService jwkService;

    @Autowired
    public JwksController(JwkService jwkService) {
        this.jwkService = jwkService;
    }

    @GetMapping
    public Map<String, Object> getJwks() {
        return jwkService.getAllActiveJwks();
    }
}
