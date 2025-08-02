package com.bbmovie.email.service.kafka;

import com.bbmovie.email.config.KafkaTopicConfig;
import com.bbmovie.email.service.email.EmailService;
import com.bbmovie.email.service.email.EmailServiceFactory;
import com.example.common.dtos.kafka.NotificationEvent;
import com.example.common.enums.NotificationType;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Log4j2
@Service
public class AuthServiceEventListener {

    private final EmailServiceFactory emailServiceFactory;

    @Autowired
    public AuthServiceEventListener( EmailServiceFactory emailServiceFactory) {
        this.emailServiceFactory = emailServiceFactory;
    }

    @KafkaListener(topics = KafkaTopicConfig.REGISTER_EMAIL_TOPIC, groupId = "email-service-group")
    public void consumeRegistrationMagicLink(
            ConsumerRecord<String, Map<String, String>> consumerRecord,
            Acknowledgment acknowledgment
    ) {
        trySendEmail(consumerRecord, (strategy, event) -> {
            String email = consumerRecord.key();
            String token = event.get("token");
            strategy.sendVerificationEmail(email, token);
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = KafkaTopicConfig.FORGOT_PASSWORD_TOPIC, groupId = "email-service-group")
    public void consumeForgotPasswordMagicLink(
            ConsumerRecord<String, Map<String, String>> consumerRecord,
            Acknowledgment acknowledgment
    ) {
        trySendEmail(consumerRecord, (strategy, event) -> {
            String email = consumerRecord.key();
            String token = event.get("token");
            strategy.sendForgotPasswordEmail(email, token);
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = KafkaTopicConfig.CHANGE_PASSWORD_EMAIL_TOPIC, groupId = "email-service-group")
    public void consumePasswordChangeNotification(
            ConsumerRecord<String, Map<String, String>> consumerRecord,
            Acknowledgment acknowledgment
    ) {
        trySendEmail(consumerRecord, (strategy, event) -> {
            String email = consumerRecord.key();
            String timeChangedPassword = event.get("timeChangedPassword");
            strategy.notifyChangedPassword(email, ZonedDateTime.parse(timeChangedPassword));
        });
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void handleNotification(NotificationEvent event, Acknowledgment acknowledgment) {
        if (event.getType().equals(NotificationType.EMAIL)) {
            log.info("Sending email for user {}: {}", event.getUserId(), event.getMessage());
            // do nothing now
        }
        acknowledgment.acknowledge();
    }

    private void trySendEmail(
            ConsumerRecord<String, Map<String, String>> consumerRecord,
            BiConsumer<EmailService, Map<String, String>> sendAction
    ) {
        Map<String, String> event = consumerRecord.value();
        String key = consumerRecord.key();
        List<EmailService> strategies = emailServiceFactory.getRotationStrategies();
        StringBuilder errorLog = new StringBuilder();

        for (EmailService strategy : strategies) {
            try {
                sendAction.accept(strategy, event);
                log.info("Successfully sent email for key {} using strategy {}", key, strategy.getClass().getSimpleName());
                return;
            } catch (Exception e) {
                String error = String.format("Failed to send email for key %s with strategy %s: %s",
                        key, strategy.getClass().getSimpleName(), e.getMessage());
                log.error(error);
                errorLog.append(error).append("; ");
            }
        }
        log.error("All email strategies failed for key {}: {}", key, errorLog.toString());
    }
}
