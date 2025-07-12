package com.example.bbmovieuploadfile.service;

import com.example.bbmovieuploadfile.entity.cdc.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.bbmovieuploadfile.repository.OutboxEventRepository;
import com.example.bbmovieuploadfile.service.ffmpeg.FFmpegVideoMetadata;
import com.example.bbmovieuploadfile.service.ffmpeg.VideoMetadataService;
import com.example.bbmovieuploadfile.service.ffmpeg.VideoTranscoderService;
import com.example.bbmovieuploadfile.service.kafka.ReactiveFileUploadEventPublisher;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import static com.example.bbmovieuploadfile.utils.FileMetaDataUtils.*;
import static com.example.bbmovieuploadfile.utils.FileUploadEventUtils.createEvent;
import static com.example.bbmovieuploadfile.constraints.ResolutionConstraints.*;

@Service
@Log4j2
public class FileUploadService {

    @Value("${spring.upload-dir}")
    private String uploadDir;

    @Value("${app.upload.temp-dir}")
    private String tempDir;

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
        String fileExtension = getExtension(originalFilename);
        String originalNameWithoutExtension = sanitizeFilenameWithoutExtension(originalFilename);
        String tempSaveName = originalNameWithoutExtension + fileExtension; // the originalFilename with extension has no postfix yet

        Path tempPath = Paths.get(tempDir, tempSaveName);
        log.info("Uploading file {} to temp directory {}", originalFilename, tempPath);

        return filePart.transferTo(tempPath)
                .then() //TODO: save temp upload to db to remove it later
                .then(metadataService.getMetadata(tempPath)) // probe before storing => get precise metadata
                .flatMap(videoMeta -> {
                    log.info("Metadata: {}", videoMeta);
                    String originalResolution = getOriginalResolution(videoMeta);
                    String fullOriginalNameWithResolutionPostFix = tempSaveName +
                            "_" +
                            originalResolution +
                            getExtension(originalFilename);

                    FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

                    return strategy.store(filePart, fullOriginalNameWithResolutionPostFix)
                            .flatMap(fileUploadResult -> {
                                log.info("Stored file {} as {}", fullOriginalNameWithResolutionPostFix, fileUploadResult.getUrl());
                                Path originalPath = Paths.get(uploadDir, fullOriginalNameWithResolutionPostFix);
                                List<VideoTranscoderService.Resolution> resolutionList =
                                        getResolutionsToTranscode(videoMeta, originalNameWithoutExtension, getExtension(originalFilename));
                                log.info("Resolutions to transcode: {}", resolutionList.stream().toList().toString());

                                return transcoderService.transcode(originalPath, resolutionList, uploadDir)
                                        .flatMap(paths -> {
                                            log.info("Transcoded paths: {}", paths.stream().map(Path::toString).toList());
                                            List<Mono<FileUploadEvent>> monos = new ArrayList<>();

                                            for (int i = 0; i < paths.size(); i++) {
                                                Path transcoded = paths.get(i);
                                                log.info("Transcoded path: {}", transcoded);
                                                VideoTranscoderService.Resolution resolution = resolutionList.get(i);
                                                String resLabel = resolution.filename().split("_")[1].replace(".mp4", "");
                                                log.info("Transcoded path: {}, quality: {}", transcoded, resLabel);

                                                if (strategy instanceof FileLocalStorageStrategy fileCapable) {
                                                    Mono<FileUploadEvent> mono = fileCapable.store(transcoded.toFile(), resolution.filename())
                                                            .flatMap(result -> {
                                                                log.info("Stored transcoded file {} as {}", resolution.filename(), result.getUrl());
                                                                FileUploadEvent event = createEvent(resolution.filename(), username, metadata, result);
                                                                event.setQuality(resLabel);
                                                                return publishEventAndOutbox(event);
                                                            });
                                                    monos.add(mono);
                                                    log.info("Executing Mono: {}", monos);
                                                }
                                            }
                                            return Mono.just(ResponseEntity.ok("Video uploaded and processed."));
                                        });
                            });
                })
                .onErrorResume(ex -> {
                    log.error("File upload process failed for {}: {}", originalFilename, ex.getMessage(), ex);
                    if (ex instanceof FileUploadException) {
                        return Mono.error(() -> new FileUploadException("Failed to upload file to server."));
                    }
                    return Mono.error(new FileUploadException("Critical error occurred during file upload process."));
                })
                .publishOn(Schedulers.boundedElastic())
                .doFinally(signal -> {
                    try {
                        //TODO: add to db to remove later if server suddenly died
                        log.info("Attempt deleting temporary file {}", tempPath);
                        Files.deleteIfExists(tempPath);
                    } catch (IOException e) {
                        log.error("Failed to delete temporary file {}: {}", tempPath, e.getMessage(), e);
                    }
                });
    }

    private List<VideoTranscoderService.Resolution> getResolutionsToTranscode(
            FFmpegVideoMetadata meta, String baseNameWithoutExtension, String extension
    ) {
        List<VideoTranscoderService.Resolution> resolutions = new ArrayList<>();
        if (meta.width() >= 1920)
            resolutions.add(new VideoTranscoderService.Resolution(1920, 1080, baseNameWithoutExtension + _1080P + extension));
        if (meta.width() >= 1280)
            resolutions.add(new VideoTranscoderService.Resolution(1280, 720, baseNameWithoutExtension + _720P + extension));
        if (meta.width() >= 854)
            resolutions.add(new VideoTranscoderService.Resolution(854, 480, baseNameWithoutExtension + _480P + extension));
        if (meta.width() >= 640)
            resolutions.add(new VideoTranscoderService.Resolution(640, 360, baseNameWithoutExtension + _360P + extension));
        if (meta.width() >= 320)
            resolutions.add(new VideoTranscoderService.Resolution(320, 240, baseNameWithoutExtension + _240P + extension));
        if (meta.width() >= 160)
            resolutions.add(new VideoTranscoderService.Resolution(160, 144, baseNameWithoutExtension + _144P + extension));
        return resolutions;
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
}