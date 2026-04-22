package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.KafkaConfig;
import com.bbmovie.notificationservice.dto.event.NotificationDeliveryTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class WebDeliveryWorker {

    private final SseService sseService;

    @KafkaListener(topics = KafkaConfig.NOTIFY_WEB, groupId = "delivery-web")
    public void deliver(NotificationDeliveryTask task) {
        log.debug("Delivering SSE to user {}: {}", task.getUserId(), task.getTitle());
        sseService.sendNotification(task.getUserId(), task);
    }
}
