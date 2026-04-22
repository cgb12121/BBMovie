package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.KafkaConfig;
import com.bbmovie.notificationservice.dto.event.NotificationDeliveryTask;
import com.bbmovie.notificationservice.dto.event.EmailNewsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class EmailDeliveryWorker {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaConfig.NOTIFY_EMAIL, groupId = "delivery-email")
    public void deliver(NotificationDeliveryTask task) {
        log.debug("Delivering Email to user {}: {}", task.getUserId(), task.getTitle());
        if (task.getEmail() != null) {
            EmailNewsEvent event = EmailNewsEvent.builder()
                    .userId(task.getUserId())
                    .email(task.getEmail())
                    .title(task.getTitle())
                    .content(task.getContent())
                    .build();
            kafkaTemplate.send(KafkaConfig.EMAIL_NEWS_EVENTS, event);
        }
    }
}
