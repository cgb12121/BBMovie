package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.dto.event.NotificationTriggerEvent;
import com.bbmovie.notificationservice.entity.Notification;
import com.bbmovie.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final KafkaProducerService kafkaProducerService;

    /**
     * Process news and trigger distributed notifications.
     * Big Tech Approach: Split Saving from Dispatching.
     */
    public void processNews(Map<String, Object> newsData) {
        String title = (String) newsData.get("title");
        String content = (String) newsData.get("content");
        String type = (String) newsData.getOrDefault("type", "NEWS");

        // 1. Save to DB (Transactional)
        Notification notification = saveNotification(title, content, type);

        // 2. Trigger Fan-out via Kafka (Non-transactional / After transaction)
        // This avoids holding DB connections while doing I/O or waiting for Kafka
        NotificationTriggerEvent triggerEvent = NotificationTriggerEvent.builder()
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .build();
        
        kafkaProducerService.triggerNotification(triggerEvent);
        
        log.info("Sent notification trigger for: {}", title);
    }

    @Transactional
    public Notification saveNotification(String title, String content, String type) {
        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .type(type)
                .build();
        return notificationRepository.save(notification);
    }
}
