package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.entitlement_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.entitlement_service.domain.EntitlementStatus;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementEventInboxEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.entity.EntitlementRecordEntity;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementEventInboxRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementRecordRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementProjectionServiceTest {

    @Mock
    private EntitlementEventInboxRepository inboxRepository;
    @Mock
    private EntitlementRecordRepository recordRepository;
    @Mock
    private EntitlementDecisionService decisionService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EntitlementProjectionService projectionService;

    @Test
    void should_project_active_entitlement_on_payment_succeeded() {
        when(inboxRepository.existsByEventId("ev-1")).thenReturn(false);
        when(recordRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                eq("user-1"),
                eq(EntitlementStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-1",
                Map.of("userId", "user-1", "planId", "premium_monthly")
        );

        projectionService.ingest("ev-1", envelope);

        ArgumentCaptor<EntitlementRecordEntity> captor = ArgumentCaptor.forClass(EntitlementRecordEntity.class);
        verify(recordRepository).save(captor.capture());
        EntitlementRecordEntity saved = captor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals("pay-1", saved.getSourcePaymentId());
        assertEquals(EntitlementStatus.ACTIVE, saved.getStatus());
        assertEquals("PREMIUM", saved.getTier());

        verify(inboxRepository).save(any(EntitlementEventInboxEntity.class));
    }

    @Test
    void should_skip_duplicate_event() {
        when(inboxRepository.existsByEventId("dup")).thenReturn(true);
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-dup",
                Map.of("userId", "user-1", "planId", "premium_monthly")
        );

        projectionService.ingest("dup", envelope);
        verify(recordRepository, never()).save(any(EntitlementRecordEntity.class));
        verify(inboxRepository, never()).save(any(EntitlementEventInboxEntity.class));
    }

    @Test
    void should_fail_when_user_id_missing() {
        when(inboxRepository.existsByEventId("ev-2")).thenReturn(false);
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-2",
                Map.of("planId", "premium_monthly")
        );

        assertThrows(IllegalArgumentException.class, () -> projectionService.ingest("ev-2", envelope));
    }
}
