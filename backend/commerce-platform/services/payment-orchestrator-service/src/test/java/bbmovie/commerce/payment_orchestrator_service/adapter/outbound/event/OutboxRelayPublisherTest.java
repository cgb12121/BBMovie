package bbmovie.commerce.payment_orchestrator_service.adapter.outbound.event;

import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.OutboxEventEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxRelayPublisher outboxRelayPublisher;

    @Test
    void should_mark_event_sent_on_successful_publish() {
        ReflectionTestUtils.setField(outboxRelayPublisher, "relayEnabled", true);
        ReflectionTestUtils.setField(outboxRelayPublisher, "kafkaEnabled", true);
        ReflectionTestUtils.setField(outboxRelayPublisher, "topic", "commerce.payment.events.v1");

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId("outbox-1");
        event.setPaymentId("pay-1");
        event.setPayloadJson("{\"eventType\":\"PaymentSucceededV1\"}");
        event.setStatus("PENDING");
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(Instant.now());

        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq("PENDING"), any()))
                .thenReturn(List.of(event));

        outboxRelayPublisher.relayBatch();

        assertEquals("SENT", event.getStatus());
        assertNotNull(event.getPublishedAt());
        verify(outboxEventRepository).saveAll(List.of(event));
    }

    @Test
    void should_increment_attempt_and_keep_pending_on_publish_failure() {
        ReflectionTestUtils.setField(outboxRelayPublisher, "relayEnabled", true);
        ReflectionTestUtils.setField(outboxRelayPublisher, "kafkaEnabled", true);
        ReflectionTestUtils.setField(outboxRelayPublisher, "topic", "commerce.payment.events.v1");

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId("outbox-2");
        event.setPaymentId("pay-2");
        event.setPayloadJson("{\"eventType\":\"PaymentFailedV1\"}");
        event.setStatus("PENDING");
        event.setAttempts(0);
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(Instant.now());

        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(outboxEventRepository.findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(eq("PENDING"), any()))
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("broker unavailable"))
                .when(kafkaTemplate).send(any(String.class), any(String.class), any(String.class));

        outboxRelayPublisher.relayBatch();

        assertEquals("PENDING", event.getStatus());
        assertEquals(1, event.getAttempts());
        assertNotNull(event.getLastError());
        assertTrue(event.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OutboxEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxEventRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
    }
}
