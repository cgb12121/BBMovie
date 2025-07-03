package com.example.bbmovieuploadfile.service.scheduled;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import com.example.bbmovieuploadfile.repository.OutboxEventRepository;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Log4j2(topic = "OutboxService")
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, FileUploadEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, FileUploadEvent> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(fixedDelay = 30_000)
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatus(OutboxStatus.PENDING);
        for (OutboxEvent event : events) {
            if (event.getRetryCount() >= 5) {
                continue;
            }
            try {
                FileUploadEvent uploadEvent = objectMapper.readValue(event.getPayload(), FileUploadEvent.class);
                kafkaTemplate.send("upload-events", event.getAggregateId(), uploadEvent);
                event.setStatus(OutboxStatus.SENT);
                event.setSentAt(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to publish outbox event with ID {}: {}", event.getId(), e.getMessage());
                event.setStatus(OutboxStatus.FAILED);
                event.setRetryCount(event.getRetryCount() + 1);
            }
            event.setLastAttemptAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 30_000)
    public void cleanSentEvents() {
        outboxEventRepository.deleteAllByStatus(OutboxStatus.SENT);
    }
}
