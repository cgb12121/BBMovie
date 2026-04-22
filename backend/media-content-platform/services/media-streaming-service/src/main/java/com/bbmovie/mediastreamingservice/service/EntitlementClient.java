package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.dto.EntitlementCheckRequest;
import com.bbmovie.mediastreamingservice.dto.EntitlementDecisionResponse;
import com.bbmovie.mediastreamingservice.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EntitlementClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${entitlement-service.base-url:http://localhost:8098}")
    private String entitlementBaseUrl;
    @Value("${entitlement-service.internal-api-key:entitlement-internal-key}")
    private String internalApiKey;

    public String resolveTierOrDeny(String userId, UUID movieId, String action) {
        EntitlementCheckRequest request = new EntitlementCheckRequest(userId, movieId.toString(), action, null);
        try {
            EntitlementDecisionResponse decision = restClientBuilder.build()
                    .post()
                    .uri(entitlementBaseUrl + "/api/v1/entitlements/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .body(request)
                    .retrieve()
                    .body(EntitlementDecisionResponse.class);

            if (decision == null || !decision.allowed()) {
                String reason = decision == null ? "NO_RESPONSE" : decision.reasonCode();
                throw new AccessDeniedException("Entitlement denied: " + reason);
            }
            return decision.tier() == null || decision.tier().isBlank() ? "FREE" : decision.tier();
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Entitlement service unavailable, deny by default for userId={} movieId={}", userId, movieId, ex);
            throw new AccessDeniedException("Entitlement check unavailable");
        }
    }
}
