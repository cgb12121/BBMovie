package com.example.bbmovie.service.kafka;

import com.example.bbmovie.service.movie.MovieService;
import com.example.common.dtos.kafka.FileUploadEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class FileUploadEventListener {

    private final MovieService movieService;
    private final KafkaTemplate<String, Object> kafkaTemplateForDLT;

    @Autowired
    public FileUploadEventListener(
            MovieService movieService,
            @Qualifier("kafkaTemplateForDLT") KafkaTemplate<String, Object> kafkaTemplateForDLT
    ) {
        this.movieService = movieService;
        this.kafkaTemplateForDLT = kafkaTemplateForDLT;
    }

    @KafkaListener(topics = "upload-events", groupId = "upload-core-group")
    public void handleUploadEvent(FileUploadEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received upload event: {}", event);
            movieService.handleFileUpload(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process message: {}", event, e);
            kafkaTemplateForDLT.send("file.uploaded.DLT", event);
            throw e;
        }
    }

    @KafkaListener(topics = "file.uploaded.DLT", groupId = "core-service-dlt")
    public void handleDlt(FileUploadEvent event, Acknowledgment acknowledgment) {
        log.error("Failed to process message. Moved to DLT: {}", event);
        // I dont know what to do with the message in DLT
    }
}
