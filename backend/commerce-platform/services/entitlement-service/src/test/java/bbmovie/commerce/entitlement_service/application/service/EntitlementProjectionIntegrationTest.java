package bbmovie.commerce.entitlement_service.application.service;

import bbmovie.commerce.commerce_contracts.contracts.payment.PaymentEventTypes;
import bbmovie.commerce.entitlement_service.application.dto.PaymentEventEnvelope;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementEventInboxRepository;
import bbmovie.commerce.entitlement_service.infrastructure.persistence.repo.EntitlementRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class EntitlementProjectionIntegrationTest {

    @Autowired
    private EntitlementProjectionService projectionService;
    @Autowired
    private EntitlementEventInboxRepository inboxRepository;
    @Autowired
    private EntitlementRecordRepository recordRepository;

    @BeforeEach
    void setup() {
        inboxRepository.deleteAll();
        recordRepository.deleteAll();
    }

    @Test
    void should_project_duplicate_event_only_once() {
        PaymentEventEnvelope envelope = new PaymentEventEnvelope(
                PaymentEventTypes.PAYMENT_SUCCEEDED_V1,
                "pay-dup-1",
                Map.of("userId", "user-dup-1", "planId", "premium_monthly")
        );

        projectionService.ingest("event-dup-1", envelope);
        projectionService.ingest("event-dup-1", envelope);

        assertEquals(1, inboxRepository.count());
        assertEquals(1, recordRepository.count());
    }
}
