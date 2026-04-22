package com.bbmovie.revenuedashboard.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "revenue.analytics.messaging.kafka.enabled", havingValue = "true")
public class PaymentEventKafkaConsumer {
    private final ObjectMapper objectMapper;
    private final JdbcTemplate clickHouseJdbcTemplate;

    @KafkaListener(topics = "${revenue.analytics.messaging.kafka.topic:commerce.payment.events.v1}")
    public void handle(ConsumerRecord<String, String> record) {
        String message = record.value();
        try {
            JsonNode root = objectMapper.readTree(message);
            JsonNode payload = root.path("payload");
            PaymentEvent event = new PaymentEvent(
                    root.path("eventType").asText("payment.unknown"),
                    root.path("paymentId").asText(null),
                    payload.path("userId").asText(null),
                    payload.path("subscriptionId").asText(payload.path("subscriptionCampaignId").asText(null)),
                    payload.path("planId").asText(null),
                    payload.path("planTier").asText(payload.path("purpose").asText(null)),
                    decimalOrZero(payload.path("amount")),
                    payload.path("currency").asText("USD"),
                    payload.path("provider").asText(null),
                    payload.path("billingCycle").asText("UNKNOWN"),
                    LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneOffset.UTC)
            );
            persist(event);
        } catch (Exception ex) {
            log.error("Failed to process kafka payment event payload={}", message, ex);
        }
    }

    private void persist(PaymentEvent event) {
        String sql = """
                INSERT INTO revenue_events
                (event_type, transaction_id, user_id, subscription_id, plan_id, plan_type,
                 amount, currency, provider, billing_cycle, event_timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        clickHouseJdbcTemplate.update(sql,
                event.eventType(),
                event.transactionId(),
                event.userId(),
                event.subscriptionId(),
                event.planId(),
                event.planType(),
                event.amount(),
                event.currency(),
                event.provider(),
                event.billingCycle(),
                event.eventTimestamp()
        );
    }

    private BigDecimal decimalOrZero(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BigDecimal.ZERO;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        return new BigDecimal(node.asText("0"));
    }
}
