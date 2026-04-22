package bbmovie.commerce.subscription_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.subscription_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.subscription_service.domain.SubscriptionStatus;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.PlanEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.SubscriptionEventInboxEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.UserSubscriptionEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.CampaignRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.PlanRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.SubscriptionEventInboxRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.UserSubscriptionRepository;
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
class SubscriptionLifecycleServiceTest {

    @Mock
    private SubscriptionEventInboxRepository inboxRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @InjectMocks
    private SubscriptionLifecycleService lifecycleService;

    @Test
    void should_create_subscription_on_payment_succeeded() {
        PlanEntity plan = new PlanEntity();
        plan.setPlanId("plan_monthly");
        plan.setDurationDays(30);
        plan.setActive(true);

        when(inboxRepository.existsByEventId("ev-1")).thenReturn(false);
        when(planRepository.findByPlanId("plan_monthly")).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserIdAndStatusAndEndsAtAfterOrderByEndsAtDesc(
                eq("user-1"),
                eq(SubscriptionStatus.ACTIVE),
                any(Instant.class)
        )).thenReturn(Optional.empty());

        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-1",
                Map.of("userId", "user-1", "planId", "plan_monthly")
        );

        lifecycleService.ingest("ev-1", envelope);

        ArgumentCaptor<UserSubscriptionEntity> subscriptionCaptor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(subscriptionCaptor.capture());
        UserSubscriptionEntity saved = subscriptionCaptor.getValue();
        assertEquals("user-1", saved.getUserId());
        assertEquals("plan_monthly", saved.getPlanId());
        assertEquals("pay-1", saved.getSourcePaymentId());
        assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());

        ArgumentCaptor<SubscriptionEventInboxEntity> inboxCaptor = ArgumentCaptor.forClass(SubscriptionEventInboxEntity.class);
        verify(inboxRepository).save(inboxCaptor.capture());
        assertEquals("ev-1", inboxCaptor.getValue().getEventId());
    }

    @Test
    void should_skip_duplicate_event() {
        when(inboxRepository.existsByEventId("ev-dup")).thenReturn(true);

        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-1",
                Map.of("userId", "user-1", "planId", "plan_monthly")
        );

        lifecycleService.ingest("ev-dup", envelope);

        verify(userSubscriptionRepository, never()).save(any(UserSubscriptionEntity.class));
        verify(inboxRepository, never()).save(any(SubscriptionEventInboxEntity.class));
    }

    @Test
    void should_fail_when_plan_unknown() {
        when(inboxRepository.existsByEventId("ev-2")).thenReturn(false);
        when(planRepository.findByPlanId("unknown")).thenReturn(Optional.empty());

        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-2",
                Map.of("userId", "user-1", "planId", "unknown")
        );

        assertThrows(IllegalArgumentException.class, () -> lifecycleService.ingest("ev-2", envelope));
    }
}
