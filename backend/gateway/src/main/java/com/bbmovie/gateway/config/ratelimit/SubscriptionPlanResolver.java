package com.bbmovie.gateway.config.ratelimit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A Spring service bean responsible for resolving rate limiting plans and keys.
 * It can be referenced in SpEL expressions in application.yml via the bean name "@subscriptionPlanResolver".
 */
@Log4j2
@Service("subscriptionPlanResolver")
public class SubscriptionPlanResolver {

    private final ObjectMapper objectMapper;

    @Autowired
    public SubscriptionPlanResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getClaims(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.isEmpty()) {
            return Collections.emptyMap();
        }

        String token = authHeaders.getFirst().replace("Bearer ", "");
        String[] parts = token.split("\\.");

        if (parts.length < 2) {
            return Collections.emptyMap();
        }

        try {
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(decodedPayload, new TypeReference<>() {});
        } catch (IllegalArgumentException | IOException e) {
            log.warn("Failed to parse JWT payload: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}