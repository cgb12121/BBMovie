package com.bbmovie.gateway.config.ratelimit;

import com.example.common.entity.JoseConstraint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Spring service bean responsible for resolving rate limiting plans and keys.
 * It can be referenced in SpEL expressions in application.yml via the bean name "@subscriptionPlanResolver".
 */
@EnableCaching
@Log4j2
@Service("subscriptionPlanResolver")
public class SubscriptionPlanResolver {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resolves the subscription plan (e.g., ANONYMOUS, FREE, PREMIUM) from the JWT token.
     * @param request The incoming server request.
     * @return The subscription plan in uppercase, or "ANONYMOUS" as a fallback.
     */
    public String resolvePlan(ServerHttpRequest request) {
        log.info("Resolving subscription plan for request: {}", request.getURI());
        return getClaim(request, JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER)
                .map(String::toUpperCase)
                .orElse("ANONYMOUS");
    }

    /**
     * Resolves a unique key for rate limiting, preferring the session ID (sid) from the JWT.
     * Falls back to the client's IP address if the token or sid is not available.
     * @param request The incoming server request.
     * @return A unique key for the user/client.
     */
    public String resolveKey(ServerHttpRequest request) {
        log.info("Resolving rate limit key for request: {}", request.getURI());
        return getClaim(request, JoseConstraint.JosePayload.SID)
                .orElseGet(() -> Optional.ofNullable(request.getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .orElse("anonymous-fallback"));
    }

    private Optional<String> getClaim(ServerHttpRequest request, String claimName) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.isEmpty()) {
            return Optional.empty();
        }

        String token = authHeaders.getFirst().replace("Bearer ", "");
        String[] parts = token.split("\\.");

        if (parts.length < 2) {
            return Optional.empty();
        }

        try {
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payloadMap = objectMapper.readValue(decodedPayload, new TypeReference<>() {});
            return Optional.ofNullable((String) payloadMap.get(claimName));
        } catch (IllegalArgumentException | IOException e) {
            log.warn("Failed to parse JWT payload: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
