package com.bbmovie.fileservice.service.upload;

import com.bbmovie.fileservice.entity.TempFileRecord;
import com.bbmovie.fileservice.entity.cdc.OutboxEvent;
import com.bbmovie.fileservice.entity.cdc.OutboxStatus;
import com.bbmovie.fileservice.exception.FileUploadException;
import com.bbmovie.fileservice.repository.OutboxEventRepository;
import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import com.bbmovie.fileservice.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.fileservice.service.ffmpeg.VideoMetadataService;
import com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.fileservice.service.kafka.ReactiveFileUploadEventPublisher;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import com.bbmovie.fileservice.service.storage.LocalStorageStrategy;
import com.bbmovie.fileservice.service.storage.LocalStorageStrategyImpl;
import com.bbmovie.fileservice.service.storage.StorageStrategy;
import com.example.common.dtos.kafka.FileUploadEvent;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.enums.Storage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.bbmovie.fileservice.constraints.ResolutionConstraints.*;
import static com.bbmovie.fileservice.utils.FileMetaDataUtils.*;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createEvent;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createNewTempUploadEvent;
import static org.apache.commons.io.FilenameUtils.getExtension;


@Log4j2
@Service("localUploadService")
public class LocalDiskUploadService implements FileUploadStrategyService {

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
    private final TempFileRecordRepository tempFileRecordRepository;

    @Autowired
    public LocalDiskUploadService(
            ReactiveFileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            VideoMetadataService metadataService,
            VideoTranscoderService transcoderService,
            TempFileRecordRepository tempFileRecordRepository
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.transcoderService = transcoderService;
        this.tempFileRecordRepository = tempFileRecordRepository;
    }

    @Transactional
    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        // Temporarily enforce the storage strategy to be LOCAL directly
        if (metadata.getStorage() == null || !Storage.LOCAL.name().equalsIgnoreCase(metadata.getStorage().name())) {
            return Mono.error(new FileUploadException("Storage strategy not supported."));
        }
        StorageStrategy strategy = storageStrategyFactory.getStrategy(Storage.LOCAL.name());

        String username = auth.getName();
        String originalFilename = filePart.filename();
        String fileExtension = getExtension(originalFilename);
        String originalNameWithoutExtension = FilenameUtils.removeExtension(originalFilename);
        String tempSaveName = sanitizeFilenameWithoutExtension(originalNameWithoutExtension) + fileExtension; // the originalFilename with extension has no postfix yet

        Path tempPath = Paths.get(tempDir, tempSaveName);
        log.info("Uploading file {} to temp directory {}", originalFilename, tempPath);

        return filePart.transferTo(tempPath)
                .then(Mono.defer(() ->{
                    TempFileRecord tempFileRecord = createNewTempUploadEvent(metadata, originalNameWithoutExtension, fileExtension, tempPath, username);
                    return tempFileRecordRepository.saveTempFile(tempFileRecord);
                }))
                .then(metadataService.getMetadata(tempPath)) // probe before storing => get precise metadata
                .flatMap(videoMeta -> {
                    log.info("Metadata: {}", videoMeta);
                    String originalResolution = getOriginalResolution(videoMeta);
                    String fullOriginalNameWithResolutionPostFix = tempSaveName +
                            "_" +
                            originalResolution +
                            getExtension(originalFilename);

                    return strategy.store(filePart, fullOriginalNameWithResolutionPostFix)
                            .flatMap(fileUploadResult -> {
                                log.info("Stored file {} as {}", fullOriginalNameWithResolutionPostFix, fileUploadResult.getUrl());
                                Path originalPath = Paths.get(uploadDir, fullOriginalNameWithResolutionPostFix);
                                List<VideoTranscoderService.VideoResolution> videoResolutionList =
                                        getResolutionsToTranscode(videoMeta, originalNameWithoutExtension, getExtension(originalFilename));
                                log.info("Resolutions to transcode: {}", videoResolutionList.stream().toList().toString());

                                return transcoderService.transcode(originalPath, videoResolutionList, uploadDir)
                                        .flatMap(paths -> {
                                            log.info("Transcoded paths: {}", paths.stream().map(Path::toString).toList());
                                            List<Mono<FileUploadEvent>> monos = new ArrayList<>();

                                            for (int i = 0; i < paths.size(); i++) {
                                                Path transcoded = paths.get(i);
                                                log.info("Transcoded path: {}", transcoded);
                                                VideoTranscoderService.VideoResolution videoResolution = videoResolutionList.get(i);
                                                String resLabel = videoResolution.filename().split("_")[1].replace(".mp4", "");
                                                log.info("Transcoded path: {}, quality: {}", transcoded, resLabel);

                                                if (strategy instanceof LocalStorageStrategy fileCapable) {
                                                    Mono<FileUploadEvent> mono = fileCapable.store(transcoded.toFile(), videoResolution.filename())
                                                            .flatMap(result -> {
                                                                log.info("Stored transcoded file {} as {}", videoResolution.filename(), result.getUrl());
                                                                FileUploadEvent event = createEvent(videoResolution.filename(), username, metadata, result);
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
                });
    }

    @Transactional
    @SuppressWarnings("squid:S3776")
    public Flux<String> uploadWithProgress(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        // Temporarily enforce the storage strategy to be LOCAL directly
        if (metadata.getStorage() == null || !Storage.LOCAL.name().equalsIgnoreCase(metadata.getStorage().name())) {
            return Flux.error(new FileUploadException("Storage strategy not supported."));
        }
        StorageStrategy strategy = storageStrategyFactory.getStrategy(Storage.LOCAL.name());

        String username = auth.getName();
        String originalFilename = filePart.filename();
        String fileExtension = getExtension(originalFilename);
        String originalNameWithoutExtension = sanitizeFilenameWithoutExtension(originalFilename);
        String tempSaveName = originalNameWithoutExtension + fileExtension;

        Path tempPath = Paths.get(tempDir, tempSaveName);

        return Flux.create(sink -> {
            sink.next("⏳ Starting upload: " + originalFilename);

            filePart.transferTo(tempPath)
                    .then(Mono.defer(() -> {
                        TempFileRecord newRecord = createNewTempUploadEvent(metadata, originalNameWithoutExtension, fileExtension, tempPath, username);
                        return tempFileRecordRepository.saveTempFile(newRecord);
                    }))
                    .then(metadataService.getMetadata(tempPath))
                    .flatMapMany(videoMeta -> {
                        sink.next("📦 Metadata probed: " + videoMeta.width() + "x" + videoMeta.height());

                        String originalResolution = getOriginalResolution(videoMeta);
                        String fullNameWithRes = tempSaveName + "_" + originalResolution + getExtension(originalFilename);
                        Path originalPath = Paths.get(uploadDir, fullNameWithRes);

                        return strategy.store(tempPath.toFile(), fullNameWithRes)
                                .doOnNext(temp -> sink.next("✅ Original video stored."))
                                .flatMapMany(temp -> {
                                    List<VideoTranscoderService.VideoResolution> videoResolutions = getResolutionsToTranscode(videoMeta, tempSaveName, getExtension(originalFilename));
                                    return transcoderService.transcode(originalPath, videoResolutions, uploadDir)
                                            .doOnNext(paths -> sink.next("🎞️ Transcoding complete for " + paths.size() + " resolutions"))
                                            .flatMapMany(paths -> {
                                                List<Mono<FileUploadEvent>> events = new ArrayList<>();
                                                for (int i = 0; i < paths.size(); i++) {
                                                    Path transcoded = paths.get(i);
                                                    VideoTranscoderService.VideoResolution videoResolution = videoResolutions.get(i);
                                                    String label = extractLabel(videoResolution.filename());
                                                    log.info("{} | {}", videoResolution.filename(), label);

                                                    if (strategy instanceof LocalStorageStrategyImpl fileCapable) {
                                                        events.add(fileCapable.store(transcoded.toFile(), videoResolution.filename())
                                                                .doOnNext(r -> sink.next("📤 Uploaded " + label + " version."))
                                                                .flatMap(result -> {
                                                                    FileUploadEvent event = createEvent(videoResolution.filename(), username, metadata, result);
                                                                    event.setQuality(label);
                                                                    log.info("Event {}: {}", label, event);
                                                                    return publishEventAndOutbox(event);
                                                                })
                                                                .doOnError(ex -> sink.next("⚠️ Failed to process " + label + " version: " + ex.getMessage()))
                                                                .onErrorResume(ex -> Mono.empty()) // Continue on error
                                                                .timeout(Duration.ofSeconds(300), Mono.empty())); // Prevent hangs
                                                    } else {
                                                        sink.next("⚠️ Skipped " + label + " version: storage strategy not supported.");
                                                    }
                                                }

                                                if (events.isEmpty()) {
                                                    sink.next("⚠️ No transcoded files were processed for storage.");
                                                    return Flux.just("🎉 Upload complete (no transcoding performed).");
                                                }

                                                return Flux.merge(events)
                                                        .map(event -> "📥 Published event for " + event.getTitle())
                                                        .concatWith(Flux.just("🎉 Upload & processing complete."));
                                            });
                                });
                    })
                    .doOnError(ex -> {
                        log.error("Upload failed for {}: {}", originalFilename, ex.getMessage(), ex);
                        sink.error(new FileUploadException("❌ Upload failed: " + ex.getMessage()));
                    })
                    .doFinally(signalType -> sink.complete())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            sink::next, // Forward emissions to sink
                            sink::error, // Forward errors to sink
                            () -> {} // Completion handled in doFinally
                    );
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    private List<VideoTranscoderService.VideoResolution> getResolutionsToTranscode(
            FFmpegVideoMetadata meta, String baseNameWithoutExtension, String extension
    ) {
        List<VideoTranscoderService.VideoResolution> videoResolutions = new ArrayList<>();
        if (meta.width() >= 1920)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(1920, 1080, baseNameWithoutExtension + _1080P + extension));
        if (meta.width() >= 1280)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(1280, 720, baseNameWithoutExtension + _720P + extension));
        if (meta.width() >= 854)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(854, 480, baseNameWithoutExtension + _480P + extension));
        if (meta.width() >= 640)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(640, 360, baseNameWithoutExtension + _360P + extension));
        if (meta.width() >= 320)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(320, 240, baseNameWithoutExtension + _240P + extension));
        if (meta.width() >= 160)
            videoResolutions.add(new VideoTranscoderService.VideoResolution(160, 144, baseNameWithoutExtension + _144P + extension));
        return videoResolutions;
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