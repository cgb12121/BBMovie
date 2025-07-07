package com.example.bbmovieuploadfile.service;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.bbmovieuploadfile.repository.OutboxEventRepository;
import com.example.bbmovieuploadfile.service.kafka.ReactiveFileUploadEventPublisher;
import com.example.common.dtos.kafka.FileUploadResult;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2
public class FileUploadService {

    private final ReactiveFileUploadEventPublisher fileUploadEventPublisher;
    private final FileStorageStrategyFactory storageStrategyFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;


    @Autowired
    public FileUploadService(
            ReactiveFileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            R2dbcEntityTemplate r2dbcEntityTemplate
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String safeName = sanitizeFilename(originalFilename);

        FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

        return strategy.store(filePart, safeName)
                .flatMap(fileUploadResult -> {
                    FileUploadEvent event = createEvent(safeName, username, metadata, fileUploadResult);
                    return createOutboxEvent(event)
                            .doOnSuccess(initialSavedOutboxEvent ->
                                log.info("OutboxEvent should now exist in DB with ID: {}", initialSavedOutboxEvent.getId())
                            )
                            .flatMap(outboxEvent -> fileUploadEventPublisher.publish(event)
                                    .then(Mono.defer(() -> {
                                        outboxEvent.setStatus(OutboxStatus.SENT);
                                        outboxEvent.setSentAt(LocalDateTime.now());
                                        outboxEvent.setSentAt(LocalDateTime.now());
                                        return outboxEventRepository.save(outboxEvent);
                                    }))
                                    .thenReturn(ResponseEntity.ok("Uploaded file successfully"))
                                    .onErrorResume(kafkaEx -> {
                                        log.error("Failed to publish FileUploadEvent to Kafka for file {}: {}", safeName, kafkaEx.getMessage(), kafkaEx);
                                        outboxEvent.setStatus(OutboxStatus.FAILED);
                                        outboxEvent.setLastAttemptAt(LocalDateTime.now());
                                        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
                                        return outboxEventRepository.save(outboxEvent)
                                                .then(Mono.error(
                                                        new FileUploadException("Failed to publish event after file upload.")
                                                ));
                                    }));
                })
                .onErrorResume(ex -> {
                    log.error("File upload process failed for {}: {}", originalFilename, ex.getMessage(), ex);
                    if (ex instanceof FileUploadException) {
                        return Mono.error(ex);
                    }
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

    private Mono<OutboxEvent> createOutboxEvent(FileUploadEvent event) {
        return Mono.defer(() -> {
            OutboxEvent outboxEvent;
            try {
                String payload = objectMapper.writeValueAsString(event);
                log.info("Event [Length {}]: {}", payload.length(), payload);
                outboxEvent = OutboxEvent.builder()
                        .id(UUID.randomUUID().toString())
                        .aggregateType(event.getEntityType().name())
                        .aggregateId(event.getTitle())
                        .eventType("FileUploadEvent")
                        .payload(payload)
                        .status(OutboxStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize FileUploadEvent: {}", e.getMessage(), e);
                return Mono.error(new FileUploadException("Failed to serialize event payload."));
            }
            return outboxEventRepository.insertOutboxEvent(
                            outboxEvent.getId(),
                            outboxEvent.getAggregateType(),
                            outboxEvent.getAggregateId(),
                            outboxEvent.getEventType(),
                            outboxEvent.getPayload(),
                            outboxEvent.getStatus(),
                            outboxEvent.getRetryCount(),
                            outboxEvent.getCreatedAt(),
                            outboxEvent.getLastAttemptAt(),
                            outboxEvent.getSentAt()
                    )
                    .doOnSuccess(v -> log.info("OutboxEvent with ID {} successfully inserted as PENDING.", outboxEvent.getId()))
                    .then(Mono.just(outboxEvent));
        });
    }

    private FileUploadEvent createEvent(@NonNull String fileName, @NonNull String uploader,
                                        @NonNull UploadMetadata metadata, @NonNull FileUploadResult result) {
        return FileUploadEvent.builder().title(fileName).entityType(metadata.getEntityType())
                .storage(metadata.getStorage()).url(result.getUrl()).publicId(result.getPublicId())
                .quality(metadata.getQuality()).uploadedBy(uploader).timestamp(LocalDateTime.now())
                .build();
    }
}