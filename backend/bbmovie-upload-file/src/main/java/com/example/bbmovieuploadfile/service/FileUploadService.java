package com.example.bbmovieuploadfile.service;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.bbmovieuploadfile.repository.OutboxEventRepository;
import com.example.bbmovieuploadfile.service.ffmpeg.FFmpegVideoMetadata;
import com.example.bbmovieuploadfile.service.ffmpeg.VideoMetadataService;
import com.example.bbmovieuploadfile.service.ffmpeg.VideoTranscoderService;
import com.example.bbmovieuploadfile.service.kafka.ReactiveFileUploadEventPublisher;
import com.example.common.dtos.kafka.FileUploadResult;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
@SuppressWarnings({ "squid:S00115" })
public class FileUploadService {

    @Value("${spring.upload-dir}")
    private String uploadDir;

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    private static final String _1080P = "1080p";
    private static final String _720P = "720p";
    private static final String _480P = "480p";
    private static final String _360P = "360p";
    private static final String _240P = "240p";
    private static final String _144P = "144p";

    private final ReactiveFileUploadEventPublisher fileUploadEventPublisher;
    private final FileStorageStrategyFactory storageStrategyFactory;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final VideoMetadataService metadataService;
    private final VideoTranscoderService transcoderService;

    @Autowired
    public FileUploadService(
            ReactiveFileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            VideoMetadataService metadataService,
            VideoTranscoderService transcoderService
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.transcoderService = transcoderService;
    }

    @Transactional
    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String tempSaveName = sanitizeFilenameWithExtension(originalFilename); // baseName has no postfix yet

        Path tempPath = Paths.get(tempDir, tempSaveName);
        log.info("Uploading file {} to temp directory {}.", originalFilename, tempPath);

        return filePart.transferTo(tempPath)
                .then(metadataService.getMetadata(tempPath)) // probe before storing => get precise metadata
                .flatMap(videoMeta -> {
                    String originalResolution = getOriginalResolution(videoMeta);
                    List<String> availableTranscodeResolutions = getAvailableTranscodeResolutions(videoMeta);
                    String fullOriginalName = tempSaveName + "_" + originalResolution + getExtension(originalFilename);

                    FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

                    return strategy.store(filePart, fullOriginalName)
                            .flatMap(fileUploadResult -> {
                                Path originalPath = Paths.get(uploadDir, fullOriginalName);
                                metadata.setQuality(availableTranscodeResolutions);

                                List<Mono<FileUploadEvent>> transcodingMonos = addResolutions(metadata, videoMeta, originalPath, strategy, tempSaveName, username);
                                FileUploadEvent originalEvent = createEvent(fullOriginalName, username, metadata, fileUploadResult);
                                Mono<FileUploadEvent> originalMono = publishEventAndOutbox(originalEvent);

                                transcodingMonos.add(originalMono);
                                return Flux.merge(transcodingMonos).then(Mono.just(ResponseEntity.ok("Video upload & processing started")));
                            });
                })
                .onErrorResume(ex -> {
                    log.error("File upload process failed for {}: {}", originalFilename, ex.getMessage(), ex);
                    if (ex instanceof FileUploadException) {
                        return Mono.error(ex);
                    }
                    return Mono.error(new FileUploadException("Unable to upload file to server."));
                })
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signal -> {
                    try {
                        log.info("Attempt deleting temporary file {}.", tempPath);
                        Files.deleteIfExists(tempPath);
                    } catch (IOException e) {
                        log.error("Failed to delete temporary file {}: {}", tempPath, e.getMessage(), e);
                    }
                });
    }

    private List<Mono<FileUploadEvent>> addResolutions(
            UploadMetadata metadata, FFmpegVideoMetadata videoMeta, Path originalPath,
            FileStorageStrategy strategy, String nameWithoutExtension, String username
    ) {
        List<Mono<FileUploadEvent>> transcodingMonos = new ArrayList<>();
        if (videoMeta.width() >= 1920) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _1080P, 1920, 1080, metadata, username));
        }
        if (videoMeta.width() >= 1280) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _720P, 1280, 720, metadata, username));
        }
        if (videoMeta.width() >= 854) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _480P, 854, 480, metadata, username));
        }
        if (videoMeta.width() >= 640) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _360P, 640, 360, metadata, username));
        }
        if (videoMeta.width() >= 320) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _240P, 320, 240, metadata, username));
        }
        if (videoMeta.width() >= 160) {
            transcodingMonos.add(transcodeAndPublishToLocalStorage(originalPath, strategy, nameWithoutExtension, _144P, 160, 144, metadata, username));
        }
        return transcodingMonos;
    }

    @SuppressWarnings("squid:S00107")
    private Mono<FileUploadEvent> transcodeAndPublishToLocalStorage(
            Path input, FileStorageStrategy strategy, String baseNameWithExtension,
            String label, int width, int height, UploadMetadata metadata, String uploader
    ) {
        String baseNameWithoutExtension = baseNameWithExtension.substring(0, baseNameWithExtension.lastIndexOf('.'));
        String transcodedName = baseNameWithoutExtension + "_" + label + getExtension(baseNameWithExtension);
        log.info("Transcoded file name: {}", transcodedName);
        return transcoderService.transcode(
                        input,
                        Paths.get(uploadDir, transcodedName),
                        width, height
                )
                .flatMap(transcoded -> {
                    if (strategy instanceof FileCapableStorageStrategy fileCapable) {
                        log.info("Saving transcoded file {} to local storage.", transcodedName);
                        return fileCapable.store(transcoded.toFile(), transcodedName);
                    } else {
                        log.error("Saving multiple resolutions is not supported when upload to Cloud storage");
                        log.warn("Strategy {} does not support storing File: skipping {}", strategy.getStorageType(), transcodedName);
                        return Mono.empty();
                    }
                })
                .flatMap(result -> {
                    FileUploadEvent event = createEvent(transcodedName, uploader, metadata, result);
                    event.setQuality(event.getQuality());
                    return publishEventAndOutbox(event);
                });
    }

    private Mono<FileUploadEvent> publishEventAndOutbox(FileUploadEvent event) {
        return createOutboxEvent(event)
                .flatMap(outbox -> fileUploadEventPublisher.publish(event)
                        .then(Mono.defer(() -> {
                            outbox.setStatus(OutboxStatus.SENT);
                            outbox.setSentAt(LocalDateTime.now());
                            return outboxEventRepository.updateOutboxEvent(outbox);
                        }))
                        .thenReturn(event));
    }

    private String sanitizeFilenameWithExtension(String input) {
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
            return outboxEventRepository.insertOutboxEvent(outboxEvent)
                    .doOnSuccess(v -> log.info("OutboxEvent with ID {} successfully inserted as PENDING.", outboxEvent.getId()))
                    .then(Mono.just(outboxEvent));
        });
    }

    private String getOriginalResolution(FFmpegVideoMetadata meta) {
        int width = meta.width();
        if (width >= 1920) return _1080P;
        if (width >= 1280) return _720P;
        if (width >= 854)  return _480P;
        if (width >= 640)  return _360P;
        if (width >= 320)  return _240P;
        return _144P;
    }

    private List<String> getAvailableTranscodeResolutions(FFmpegVideoMetadata meta) {
        List<String> labels = new ArrayList<>();
        int width = meta.width();
        if (width >= 160)  labels.add(_144P);
        if (width >= 320)  labels.add(_240P);
        if (width >= 640)  labels.add(_360P);
        if (width >= 854)  labels.add(_480P);
        if (width >= 1280) labels.add(_720P);
        if (width >= 1920) labels.add(_1080P);
        return labels;
    }

    private String getExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex != -1 ? filename.substring(lastDotIndex) : "";
    }

    private FileUploadEvent createEvent(
            @NonNull String fileName, @NonNull String uploader,
            @NonNull UploadMetadata metadata, @NonNull FileUploadResult result
    ) {
        return FileUploadEvent.builder()
                .title(fileName)
                .entityType(metadata.getEntityType())
                .storage(metadata.getStorage())
                .url(result.getUrl())
                .publicId(result.getPublicId())
                .quality(metadata.getQuality())
                .uploadedBy(uploader)
                .timestamp(LocalDateTime.now())
                .build();
    }
}