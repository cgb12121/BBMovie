package com.bbmovie.revenuedashboard.event;

import com.bbmovie.revenuedashboard.config.RevenueAnalyticsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@ConditionalOnProperty(name = "revenue.analytics.messaging.nats.enabled", havingValue = "true")
public class PaymentEventConsumer {

    private static final String[] SUBJECTS = {
            "payments.subscription.success",
            "payments.subscription.cancel",
            "payments.subscription.renew",
            "payments.subscription.renewal.upcoming"
    };

    private final Connection natsConnection;
    private final JdbcTemplate clickHouseJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RevenueAnalyticsProperties properties;
    private Subscription[] subscriptions;
    private Dispatcher dispatcher;

    public PaymentEventConsumer(
            Connection natsConnection,
            JdbcTemplate clickHouseJdbcTemplate,
            RevenueAnalyticsProperties properties) {
        this.natsConnection = natsConnection;
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void startListening() {
        log.info("Starting NATS consumer for payment events on subjects: {}", (Object) SUBJECTS);

        dispatcher = natsConnection.createDispatcher(this::handleMessage);

        subscriptions = new Subscription[SUBJECTS.length];
        for (int i = 0; i < SUBJECTS.length; i++) {
            subscriptions[i] = (Subscription) dispatcher.subscribe(SUBJECTS[i]);
            log.info("Subscribed to NATS subject: {}", SUBJECTS[i]);
        }
    }

    @PreDestroy
    public void stopListening() throws InterruptedException, TimeoutException {
        log.info("Stopping NATS consumer...");
        if (dispatcher != null) {
            dispatcher.unsubscribe(Arrays.toString(SUBJECTS));
        }
        if (natsConnection != null && natsConnection.getStatus() == Connection.Status.CONNECTED) {
            natsConnection.flush(Duration.ofMinutes(1));
        }
    }

    private void handleMessage(Message msg) {
        try {
            String body = new String(msg.getData(), StandardCharsets.UTF_8);
            log.debug("Received message on {}: {}", msg.getSubject(), body);

            JsonNode json = objectMapper.readTree(body);

            String subject = msg.getSubject();
            String eventType = mapSubjectToEventType(subject);

            PaymentEvent event = new PaymentEvent(
                    eventType,
                    getJsonString(json, "transactionId"),
                    getJsonString(json, "userId"),
                    getJsonString(json, "subscriptionId"),
                    getJsonString(json, "planId"),
                    getJsonString(json, "planType"),
                    getJsonBigDecimal(json, "amount"),
                    getJsonString(json, "currency"),
                    getJsonString(json, "provider"),
                    getJsonString(json, "billingCycle"),
                    LocalDateTime.now(ZoneOffset.UTC)
            );

            persistEvent(event);
            log.info("Persisted payment event: type={}, txId={}, userId={}", event.eventType(), event.transactionId(), event.userId());

        } catch (Exception e) {
            log.error("Error processing NATS payment event on subject {}: {}", msg.getSubject(), e.getMessage(), e);
        }
    }

    private String mapSubjectToEventType(String subject) {
        return switch (subject) {
            case "payments.subscription.success" -> "subscription.created";
            case "payments.subscription.cancel" -> "subscription.cancelled";
            case "payments.subscription.renew" -> "subscription.renewed";
            case "payments.subscription.renewal.upcoming" -> "subscription.renewal.upcoming";
            default -> subject;
        };
    }

    private void persistEvent(PaymentEvent event) {
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

    private String getJsonString(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return (node != null && !node.isNull()) ? node.asText() : null;
    }

    private BigDecimal getJsonBigDecimal(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return (node != null && !node.isNull()) ? node.decimalValue() : BigDecimal.ZERO;
    }
}
