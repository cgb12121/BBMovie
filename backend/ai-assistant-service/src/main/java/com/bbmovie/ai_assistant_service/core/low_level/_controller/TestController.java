package com.bbmovie.ai_assistant_service.core.low_level._controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/public")
    public Mono<String> publicEndpoint() {
        return Mono.just("Public access works!");
    }

    @GetMapping("/authenticated")
    public Mono<String> authenticatedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        return Mono.just("Authenticated as: " + jwt.getSubject());
    }

    @GetMapping("/optional")
    public Mono<String> optionalAuth(
            @AuthenticationPrincipal(errorOnInvalidType = false) Jwt jwt) {
        return Mono.just(jwt != null 
            ? "Authenticated as: " + jwt.getSubject()
            : "Anonymous access");
    }
}