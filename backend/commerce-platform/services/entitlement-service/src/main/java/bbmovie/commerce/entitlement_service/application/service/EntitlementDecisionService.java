package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementCheckRequest;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementDecisionResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementExplainResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementOverrideRequest;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementRecordResponse;
import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.UserEntitlementsResponse;
import bbmovie.commerce.entitlement_service.application.rules.DecisionContext;
import bbmovie.commerce.entitlement_service.application.rules.DecisionRule;
import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import bbmovie.commerce.entitlement_service.infrastructure.cache.EntitlementDecisionCacheRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementOverrideAuditEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementOverrideAuditRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EntitlementDecisionService {

    private final EntitlementRecordRepository recordRepository;
    private final EntitlementDecisionCacheRepository cacheRepository;
    private final List<DecisionRule> rules;
    private final EntitlementOverrideAuditRepository overrideAuditRepository;

    public EntitlementDecisionResponse check(EntitlementCheckRequest request) {
        String cacheKey = "entitlement:decision:" + request.userId() + ":" + request.resourceId() + ":" + request.action()
                + ":" + (request.contentPackage() != null ? request.contentPackage() : "");
        var cached = cacheRepository.get(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        Instant now = Instant.now();
        EntitlementRecordEntity active = recordRepository
                .findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                        request.userId(),
                        EntitlementStatus.ACTIVE,
                        now
                )
                .orElse(null);

        DecisionContext context = new DecisionContext(request, active, now);
        for (DecisionRule rule : rules) {
            var result = rule.evaluate(context);
            if (!result.passed()) {
                EntitlementDecisionResponse denied = new EntitlementDecisionResponse(
                        false,
                        result.reasonCode(),
                        active == null ? null : active.getPlanId(),
                        active == null ? null : active.getTier(),
                        active == null ? null : active.getEndsAt(),
                        now
                );
                cacheRepository.put(cacheKey, denied);
                return denied;
            }
        }

        EntitlementDecisionResponse response = new EntitlementDecisionResponse(
            true,
            "ACTIVE_ENTITLEMENT",
            active.getPlanId(),
            active.getTier(),
            active.getEndsAt(),
            now
        );
        cacheRepository.put(cacheKey, response);
        return response;
    }

    public UserEntitlementsResponse getUserEntitlements(String userId) {
        List<EntitlementRecordResponse> entries = recordRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new UserEntitlementsResponse(userId, entries.size(), entries);
    }

    private EntitlementRecordResponse toResponse(EntitlementRecordEntity entity) {
        return new EntitlementRecordResponse(
                entity.getEntitlementId(),
                entity.getUserId(),
                entity.getSubscriptionId(),
                entity.getPlanId(),
                entity.getCampaignId(),
                entity.getTier(),
                entity.getStatus(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getUpdatedAt()
        );
    }

    public void evictUserCache(String userId) {
        cacheRepository.evictPrefix("entitlement:decision:" + userId + ":");
    }

    public EntitlementExplainResponse explain(EntitlementCheckRequest request) {
        EntitlementDecisionResponse decision = check(request);
        return new EntitlementExplainResponse(decision, List.of(
                "lookup-active-entitlement",
                "rule-active-subscription-gate",
                "rule-plan-content-policy",
                "decision-" + decision.reasonCode()
        ));
    }

    public void grantOverride(EntitlementOverrideRequest request, String actor) {
        Instant now = Instant.now();
        EntitlementRecordEntity entity = new EntitlementRecordEntity();
        entity.setUserId(request.userId());
        entity.setStatus(EntitlementStatus.ACTIVE);
        entity.setPlanId(request.planId() == null ? "manual_override" : request.planId());
        entity.setTier(request.tier() == null ? "PREMIUM" : request.tier());
        entity.setStartsAt(now);
        entity.setEndsAt(request.expiresAt() == null ? now.plusSeconds(86400 * 30L) : request.expiresAt());
        entity.setSourcePaymentId("override:" + now.toEpochMilli());
        entity.setUpdatedAt(now);
        entity.setLastEventAt(now);
        recordRepository.save(entity);
        audit(actor, request.userId(), "GRANT", request.reason());
        evictUserCache(request.userId());
    }

    public void revokeOverride(EntitlementOverrideRequest request, String actor) {
        recordRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                request.userId(), EntitlementStatus.ACTIVE, Instant.now()).ifPresent(entity -> {
            entity.setStatus(EntitlementStatus.REVOKED);
            entity.setEndsAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            entity.setLastEventAt(Instant.now());
            recordRepository.save(entity);
        });
        audit(actor, request.userId(), "REVOKE", request.reason());
        evictUserCache(request.userId());
    }

    private void audit(String actor, String userId, String action, String reason) {
        EntitlementOverrideAuditEntity audit = new EntitlementOverrideAuditEntity();
        audit.setActor(actor == null || actor.isBlank() ? "unknown" : actor);
        audit.setTargetUserId(userId);
        audit.setAction(action);
        audit.setReason(reason);
        audit.setCreatedAt(Instant.now());
        overrideAuditRepository.save(audit);
    }
}
