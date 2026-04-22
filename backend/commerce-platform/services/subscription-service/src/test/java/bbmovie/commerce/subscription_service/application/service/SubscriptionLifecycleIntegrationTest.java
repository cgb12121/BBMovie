package bbmovie.commerce.subscription_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.subscription_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.subscription_service.infrastructure.persistence.entity.PlanEntity;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.PlanRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.SubscriptionEventInboxRepository;
import bbmovie.commerce.subscription_service.infrastructure.persistence.repo.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class SubscriptionLifecycleIntegrationTest {

    @Autowired
    private SubscriptionLifecycleService lifecycleService;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private UserSubscriptionRepository userSubscriptionRepository;
    @Autowired
    private SubscriptionEventInboxRepository inboxRepository;

    @BeforeEach
    void setup() {
        userSubscriptionRepository.deleteAll();
        inboxRepository.deleteAll();
        planRepository.deleteAll();

        PlanEntity plan = new PlanEntity();
        plan.setPlanId("plan_monthly");
        plan.setName("Monthly");
        plan.setDurationDays(30);
        plan.setActive(true);
        planRepository.save(plan);
    }

    @Test
    void should_process_duplicate_event_only_once() {
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-dup-1",
                Map.of("userId", "user-dup-1", "planId", "plan_monthly")
        );

        lifecycleService.ingest("event-dup-1", envelope);
        lifecycleService.ingest("event-dup-1", envelope);

        assertEquals(1, inboxRepository.count());
        assertEquals(1, userSubscriptionRepository.count());
    }
}
