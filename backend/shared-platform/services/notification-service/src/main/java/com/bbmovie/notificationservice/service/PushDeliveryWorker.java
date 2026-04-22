package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.KafkaConfig;
import com.bbmovie.notificationservice.dto.event.NotificationDeliveryTask;
import com.bbmovie.notificationservice.entity.UserDeviceToken;
import com.bbmovie.notificationservice.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class PushDeliveryWorker {

    private final UserDeviceTokenRepository deviceTokenRepository;

    @KafkaListener(topics = KafkaConfig.NOTIFY_PUSH, groupId = "delivery-push")
    public void deliver(NotificationDeliveryTask task) {
        log.debug("Delivering Push to user {}: {}", task.getUserId(), task.getTitle());
        List<UserDeviceToken> tokens = deviceTokenRepository.findByUserId(task.getUserId());
        for (UserDeviceToken token : tokens) {
            // Placeholder for FCM delivery
            log.info("Would send FCM push to token: {} with content: {}", token.getFcmToken(), task.getTitle());
        }
    }
}
