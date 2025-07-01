package com.example.bbmovie.service.kafka;

import com.example.bbmovie.service.movie.MovieService;
import com.example.common.dtos.kafka.FileUploadEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileUploadEventListener {

    private final MovieService movieService;

    @Autowired
    public FileUploadEventListener(MovieService movieService) {
        this.movieService = movieService;
    }

    @KafkaListener(topics = "upload-events", groupId = "upload-core-group")
    public void handleUploadEvent(FileUploadEvent event) {
        log.info("Received upload event: {}", event);
        movieService.handleFileUpload(event);
    }

    @KafkaListener(topics = "file.uploaded.DLT", groupId = "core-service-dlt")
    public void handleDlt(FileUploadEvent event) {
        log.error("Failed to process message. Moved to DLT: {}", event);
        // You can store in DB, alert via email/slack, or retry manually
    }
}
