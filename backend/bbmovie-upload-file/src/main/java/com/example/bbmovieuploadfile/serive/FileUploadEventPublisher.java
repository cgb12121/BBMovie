package com.example.bbmovieuploadfile.serive;

import com.example.bbmovieuploadfile.dto.FileUploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileUploadEventPublisher {

    private final KafkaTemplate<String, FileUploadEvent> kafkaTemplate;

    public FileUploadEventPublisher(KafkaTemplate<String, FileUploadEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(FileUploadEvent event) {
        kafkaTemplate.send("upload-events", event.getFileType(), event)
                     .whenComplete((result, throwable) -> {
                                 if (throwable == null) {
                                     log.info("Published event: {}", result);
                                 } else {
                                     log.error("Failed to publish event", throwable);
                                 }
                    });
    }
}