package com.bbmovie.gateway.config;

import com.example.common.entity.JoseConstraint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Configuration
public class RateLimiterConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public KeyResolver customKeyResolver() {
        return exchange -> {
            List<String> authHeaders = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION);

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String token = authHeaders.getFirst().replace("Bearer ", "");
                String[] parts = token.split("\\.");

                if (parts.length >= 2) {
                    try {
                        byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
                        Map<String, Object> payloadMap = objectMapper.readValue(decodedPayload, new TypeReference<>() {});

                        String subscriptionTier = (String) payloadMap.get(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER);

                        if (subscriptionTier != null && !subscriptionTier.isBlank()) {
                            return Mono.just(subscriptionTier);
                        }
                    } catch (IllegalArgumentException | IOException e) {
                        log.warn("Failed to parse JWT payload: {}", e.getMessage());
                    }
                }
            }

            return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .map(Mono::just)
                    .orElse(Mono.just("anonymous"));
        };
    }
}