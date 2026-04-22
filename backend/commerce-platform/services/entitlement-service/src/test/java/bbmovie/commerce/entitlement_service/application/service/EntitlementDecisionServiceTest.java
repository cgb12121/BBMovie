package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.entitlement_service.adapter.inbound.rest.dto.EntitlementCheckRequest;
import bbmovie.commerce.entitlement_service.application.rules.ActiveEntitlementRule;
import bbmovie.commerce.entitlement_service.application.rules.PackagePolicyRule;
import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import bbmovie.commerce.entitlement_service.infrastructure.cache.EntitlementDecisionCacheRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementOverrideAuditRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.PlanContentPolicyRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementDecisionServiceTest {

    @Mock
    private EntitlementRecordRepository recordRepository;
    @Mock
    private EntitlementDecisionCacheRepository cacheRepository;
    @Mock
    private PlanContentPolicyRepository policyRepository;
    @Mock
    private EntitlementOverrideAuditRepository overrideAuditRepository;

    private EntitlementDecisionService decisionService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        decisionService = new EntitlementDecisionService(
                recordRepository,
                cacheRepository,
                List.of(new ActiveEntitlementRule(), new PackagePolicyRule(policyRepository)),
                overrideAuditRepository
        );
    }

    @Test
    void should_allow_when_active_entitlement_exists() {
        EntitlementRecordEntity entity = new EntitlementRecordEntity();
        entity.setEntitlementId("ent-1");
        entity.setUserId("user-1");
        entity.setPlanId("premium");
        entity.setTier("PREMIUM");
        entity.setStatus(EntitlementStatus.ACTIVE);
        entity.setEndsAt(Instant.now().plusSeconds(3600));

        when(recordRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                eq("user-1"),
                eq(EntitlementStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(Optional.of(entity));

        var decision = decisionService.check(new EntitlementCheckRequest("user-1", "movie-1", "STREAM", null));
        assertTrue(decision.allowed());
        assertEquals("ACTIVE_ENTITLEMENT", decision.reasonCode());
        assertEquals("PREMIUM", decision.tier());
    }

    @Test
    void should_deny_when_no_active_entitlement() {
        when(recordRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                eq("user-2"),
                eq(EntitlementStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        var decision = decisionService.check(new EntitlementCheckRequest("user-2", "movie-1", "STREAM", null));
        assertFalse(decision.allowed());
        assertEquals("NO_ACTIVE_ENTITLEMENT", decision.reasonCode());
    }

    @Test
    void should_return_user_entitlements() {
        EntitlementRecordEntity entity = new EntitlementRecordEntity();
        entity.setEntitlementId("ent-1");
        entity.setUserId("user-1");
        entity.setStatus(EntitlementStatus.ACTIVE);
        entity.setStartsAt(Instant.now());
        entity.setEndsAt(Instant.now().plusSeconds(100));
        entity.setUpdatedAt(Instant.now());

        when(recordRepository.findByUserIdOrderByUpdatedAtDesc("user-1")).thenReturn(List.of(entity));
        var response = decisionService.getUserEntitlements("user-1");
        assertEquals("user-1", response.userId());
        assertEquals(1, response.total());
    }
}
