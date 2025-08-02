package com.bbmovie.auth.service.kafka;

import com.example.common.dtos.kafka.NotificationEvent;
import com.example.common.enums.NotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TotpProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public TotpProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendNotification(String userId, NotificationType type, String destination, String message) {
        NotificationEvent event = new NotificationEvent(userId, type, destination, message);
        kafkaTemplate.send("notification-topic", event);
    }
}