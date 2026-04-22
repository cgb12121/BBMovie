package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.KafkaConfig;
import com.bbmovie.notificationservice.dto.event.NotificationTriggerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void triggerNotification(NotificationTriggerEvent event) {
        log.info("Triggering notification event: {}", event.getTitle());
        kafkaTemplate.send(KafkaConfig.NOTIFY_TRIGGERS, event);
    }
}
