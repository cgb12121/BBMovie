package com.example.bbmovieuploadfile.service.kafka;

import com.example.common.dtos.kafka.FileUploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class FileUploadEventPublisher implements ReactiveFileUploadEventPublisher {

    private final KafkaTemplate<String, FileUploadEvent> kafkaTemplate;

    @Autowired
    public FileUploadEventPublisher(KafkaTemplate<String, FileUploadEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> publish(FileUploadEvent event) {
        return Mono.fromCallable(() -> {
                    kafkaTemplate.send("upload-events", event.getTitle(), event)
                            .whenComplete((result, throwable) -> {
                                if (throwable == null) {
                                    log.info("Published event: {}", result);
                                } else {
                                    log.error("Failed to publish event", throwable);
                                }
                            });
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}