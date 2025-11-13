package com.bbmovie.gateway.config.ratelimit;

import com.bbmovie.common.entity.JoseConstraint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * A Spring service bean responsible for resolving rate limiting plans and keys.
 * It can be referenced in SpEL expressions in application.yml via the bean name "@claimsResolver".
 */
@Log4j2
@Service("claimsResolver")
public class ClaimsResolver {

    private final ObjectMapper objectMapper;

    @Autowired
    public ClaimsResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String resolvePlan(Map<String, Object> claims) {
        return (String) claims.getOrDefault(JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER, "ANONYMOUS");
    }

    public String resolveSID(Map<String, Object> claims, ServerHttpRequest request) {
        return (String) claims.getOrDefault(JoseConstraint.JosePayload.SID,
                Optional.ofNullable(request.getRemoteAddress())
                        .map(address -> address.getAddress().getHostAddress())
                        .orElse("ANONYMOUS"));
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