package bbmovie.commerce.entitlement_service.adapter.inbound.rest;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementCheckRequest;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementBatchCheckRequest;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementBatchCheckResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementDecisionResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementExplainResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementOverrideRequest;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.UserEntitlementsResponse;
import bbmovie.commerce.entitlement_service.application.service.EntitlementDecisionService;
import bbmovie.commerce.entitlement_service.application.service.EntitlementReplayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/entitlements")
public class EntitlementController {

    private final EntitlementDecisionService decisionService;
    private final EntitlementReplayService replayService;
    @Value("${entitlement.security.internal-api-key:entitlement-internal-key}")
    private String internalApiKey;

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPPORT')")
    public EntitlementDecisionResponse check(@RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
                                             @Valid @RequestBody EntitlementCheckRequest request) {
        validateInternalApiKey(apiKey);
        return decisionService.check(request);
    }

    @PostMapping("/check-batch")
    @PreAuthorize("hasAnyRole('ADMIN','USER','SUPPORT')")
    public EntitlementBatchCheckResponse checkBatch(@Valid @RequestBody EntitlementBatchCheckRequest request) {
        var decisions = request.items().stream().map(decisionService::check).toList();
        return new EntitlementBatchCheckResponse(decisions.size(), decisions);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public UserEntitlementsResponse getByUserId(@PathVariable String userId) {
        return decisionService.getUserEntitlements(userId);
    }

    @PostMapping("/admin/replay")
    @PreAuthorize("hasRole('ADMIN')")
    public String replayLastHour() {
        int replayed = replayService.replayRange(java.time.Instant.now().minusSeconds(3600), java.time.Instant.now());
        return "Replayed " + replayed + " events";
    }

    @PostMapping("/explain")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public EntitlementExplainResponse explain(@Valid @RequestBody EntitlementCheckRequest request) {
        return decisionService.explain(request);
    }

    @PostMapping("/admin/overrides/grant")
    @PreAuthorize("hasRole('ADMIN')")
    public String grantOverride(@Valid @RequestBody EntitlementOverrideRequest request, Authentication authentication) {
        decisionService.grantOverride(request, authentication == null ? "system" : authentication.getName());
        return "ok";
    }

    @PostMapping("/admin/overrides/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public String revokeOverride(@Valid @RequestBody EntitlementOverrideRequest request, Authentication authentication) {
        decisionService.revokeOverride(request, authentication == null ? "system" : authentication.getName());
        return "ok";
    }

    private void validateInternalApiKey(String apiKey) {
        if (apiKey == null || !apiKey.equals(internalApiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid internal API key");
        }
    }
}
