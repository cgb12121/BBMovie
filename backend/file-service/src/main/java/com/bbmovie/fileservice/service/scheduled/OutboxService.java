package com.bbmovie.fileservice.service.scheduled;

import com.bbmovie.fileservice.entity.cdc.OutboxStatus;
import com.bbmovie.fileservice.repository.OutboxEventRepository;
import com.bbmovie.fileservice.service.nats.ReactiveNatsProducer;
import com.bbmovie.common.dtos.nats.FileUploadEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Log4j2(topic = "OutboxService")
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ReactiveNatsProducer reactiveNatsProducer;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ReactiveNatsProducer reactiveNatsProducer,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.reactiveNatsProducer = reactiveNatsProducer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @Scheduled(cron = "0 0/10 * * * *")
    public void publishPendingEvents() {
        log.info("Attempting to publish pending outbox events...");

        outboxEventRepository.findByStatus(OutboxStatus.PENDING)
                .filter(event -> event.getRetryCount() < 5)
                .flatMap(event -> {
                    event.setLastAttemptAt(LocalDateTime.now());
                    return Mono.just(event)
                            .flatMap(evt -> {
                                try {
                                    FileUploadEvent uploadEvent = objectMapper.readValue(evt.getPayload(), FileUploadEvent.class);
                                    return reactiveNatsProducer.send("upload.events", evt.getAggregateId(), uploadEvent)
                                            .doOnSuccess(v -> {
                                                evt.setStatus(OutboxStatus.SENT);
                                                evt.setSentAt(LocalDateTime.now());
                                            })
                                            .onErrorResume(e -> {
                                                log.error("Failed to publish outbox event with ID {}: {}", evt.getId(), e.getMessage());
                                                evt.setStatus(OutboxStatus.FAILED);
                                                evt.setRetryCount(evt.getRetryCount() + 1);
                                                return Mono.empty();
                                            })
                                            .then(outboxEventRepository.updateOutboxEvent(evt));
                                } catch (Exception e) {
                                    log.error("Failed to deserialize payload for outbox event with ID {}: {}", evt.getId(), e.getMessage());
                                    evt.setStatus(OutboxStatus.FAILED);
                                    evt.setRetryCount(evt.getRetryCount() + 1);
                                    return outboxEventRepository.updateOutboxEvent(evt);
                                }
                            });
                })
                .doOnComplete(() -> log.info("Finished processing pending outbox events."))
                .doOnError(e -> log.error("Error during publishPendingEvents scheduled task: {}", e.getMessage(), e))
                .subscribe();
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanSentEvents() {
        log.info("Attempting to clean sent outbox events...");
        outboxEventRepository.deleteAllByStatus(OutboxStatus.SENT)
                .doOnSuccess(v -> log.info("Successfully cleaned sent outbox events."))
                .doOnError(e -> log.error("Error during cleanSentEvents scheduled task: {}", e.getMessage(), e))
                .subscribe();
    }
}
