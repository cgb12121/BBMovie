package com.example.bbmovieuploadfile.service;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.bbmovieuploadfile.repository.OutboxEventRepository;
import com.example.common.dtos.kafka.FileUploadResult;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2(topic = "FileUploadService")
public class FileUploadService {

    private final FileUploadEventPublisher fileUploadEventPublisher;
    private final FileStorageStrategyFactory storageStrategyFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public FileUploadService(
            FileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String safeName = sanitizeFilename(originalFilename);

        FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

        return strategy.store(filePart, safeName)
                .publishOn(Schedulers.boundedElastic())
                .flatMap(result -> {
                    FileUploadEvent event = createEvent(safeName, username, metadata, result);
                    return Mono.fromCallable(() -> {
                                OutboxEvent outboxEvent = createOutboxEvent(event);
                                fileUploadEventPublisher.publish(event);
                                updateOutboxEventAfterKafka(outboxEvent);
                                return ResponseEntity.ok("Uploaded file successfully");
                            })
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .onErrorResume(ex -> {
                    log.error("Upload failed: {}", ex.getMessage(), ex);
                    return Mono.error(new FileUploadException("Unable to upload file to server."));
                });
    }

    private String sanitizeFilename(String input) {
        int lastDotIndex = input.lastIndexOf('.');
        String fileNameWithoutExtension;
        String fileExtension = "";

        if (lastDotIndex > 0) {
            fileNameWithoutExtension = input.substring(0, lastDotIndex);
            fileExtension = input.substring(lastDotIndex);
        } else {
            fileNameWithoutExtension = input;
        }

        String sanitizedFileName = fileNameWithoutExtension
                .replaceAll("[^\\w\\- ]", "_") // Keep only letters, numbers, underscores, hyphens, and spaces
                .replaceAll("\\s+", ""); // Remove all consecutive spaces

        return sanitizedFileName + fileExtension;
    }

    private FileUploadEvent createEvent(@NonNull String fileName, @NonNull String uploader,
            @NonNull UploadMetadata metadata, @NonNull FileUploadResult result) {
        return FileUploadEvent.builder().title(fileName).entityType(metadata.getEntityType())
                .storage(metadata.getStorage()).url(result.getUrl()).publicId(result.getPublicId())
                .quality(metadata.getQuality()).uploadedBy(uploader).timestamp(LocalDateTime.now())
                .build();
    }

    private OutboxEvent createOutboxEvent(FileUploadEvent event) {
        OutboxEvent outboxEvent;
        try {
            String obj = objectMapper.writeValueAsString(event);
            log.info("Event [Length {}]: {}", obj.length(), obj);
            outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("MOVIE")
                    .aggregateId(event.getTitle())
                    .eventType("FileUploadEvent")
                    .payload(obj)
                    .status(OutboxStatus.PENDING)
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
        outboxEventRepository.save(outboxEvent);
        return outboxEvent;
    }

    private void updateOutboxEventAfterKafka(OutboxEvent outboxEvent) {
        outboxEvent.setStatus(OutboxStatus.SENT);
        outboxEvent.setSentAt(LocalDateTime.now());
        outboxEventRepository.save(outboxEvent);
    }
}