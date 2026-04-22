package com.bbmovie.notificationservice.service;

import com.bbmovie.notificationservice.config.KafkaConfig;
import com.bbmovie.notificationservice.dto.event.NotificationDeliveryTask;
import com.bbmovie.notificationservice.dto.event.NotificationTriggerEvent;
import com.bbmovie.notificationservice.entity.NotificationPreference;
import com.bbmovie.notificationservice.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class NotificationDispatcherWorker {

    private final NotificationPreferenceRepository preferenceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final int PAGE_SIZE = 500; // Chunking as recommended in fix.txt

    @KafkaListener(topics = KafkaConfig.NOTIFY_TRIGGERS, groupId = "notification-dispatcher")
    public void dispatch(NotificationTriggerEvent event) {
        log.info("Dispatching notification: {}", event.getTitle());

        int pageNum = 0;
        Page<NotificationPreference> page;

        do {
            page = preferenceRepository.findAll(PageRequest.of(pageNum, PAGE_SIZE));
            log.info("Processing page {} of preferences (size: {})", pageNum, page.getNumberOfElements());

            for (NotificationPreference pref : page.getContent()) {
                NotificationDeliveryTask task = NotificationDeliveryTask.builder()
                        .userId(pref.getUserId())
                        .email(pref.getUserEmail())
                        .title(event.getTitle())
                        .content(event.getContent())
                        .type(event.getType())
                        .build();

                if (pref.isWebEnabled()) {
                    kafkaTemplate.send(KafkaConfig.NOTIFY_WEB, task);
                }
                if (pref.isEmailEnabled() && pref.getUserEmail() != null) {
                    kafkaTemplate.send(KafkaConfig.NOTIFY_EMAIL, task);
                }
                if (pref.isPushEnabled()) {
                    kafkaTemplate.send(KafkaConfig.NOTIFY_PUSH, task);
                }
            }
            pageNum++;
        } while (page.hasNext());

        log.info("Finished dispatching all notifications for event: {}", event.getTitle());
    }
}
