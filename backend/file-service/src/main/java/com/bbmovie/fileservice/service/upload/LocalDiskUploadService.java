package com.bbmovie.fileservice.service.upload;

import com.bbmovie.fileservice.entity.TempFileRecord;
import com.bbmovie.fileservice.entity.cdc.OutboxEvent;
import com.bbmovie.fileservice.entity.cdc.OutboxStatus;
import com.bbmovie.fileservice.exception.FileUploadException;
import com.bbmovie.fileservice.exception.UnsupportedExtension;
import com.bbmovie.fileservice.repository.OutboxEventRepository;
import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import com.bbmovie.fileservice.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.fileservice.service.ffmpeg.VideoMetadataService;
import com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.fileservice.service.nats.ReactiveFileUploadEventPublisher;
import com.bbmovie.fileservice.service.scheduled.TempFileCleanUpService;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import com.bbmovie.fileservice.service.storage.StorageStrategy;
import com.bbmovie.fileservice.service.validation.FileValidationService;
import com.example.common.dtos.nats.FileUploadEvent;
import com.example.common.dtos.nats.UploadMetadata;
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
import java.util.stream.IntStream;

import static com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService.PREDEFINED_RESOLUTIONS;
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
    private final FileValidationService fileValidationService;
    private final TempFileCleanUpService tempFileCleanUpService;

    @Autowired
    public LocalDiskUploadService(
            ReactiveFileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            VideoMetadataService metadataService,
            VideoTranscoderService transcoderService,
            TempFileRecordRepository tempFileRecordRepository,
            FileValidationService fileValidationService,
            TempFileCleanUpService tempFileCleanUpService
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.metadataService = metadataService;
        this.transcoderService = transcoderService;
        this.tempFileRecordRepository = tempFileRecordRepository;
        this.fileValidationService = fileValidationService;
        this.tempFileCleanUpService = tempFileCleanUpService;
    }

    @Transactional
    public Mono<ResponseEntity<String>> executeFileUpload(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String fileExtension = getExtension(originalFilename);
        String nameNoExt = FilenameUtils.removeExtension(originalFilename);
        String tempSaveName = sanitizeFilenameWithoutExtension(nameNoExt) + "." + fileExtension;
        Path tempPath = Paths.get(tempDir, tempSaveName);
        StorageStrategy strategy = storageStrategyFactory.getStrategy(Storage.LOCAL.name());

        log.info("Uploading {} -> {}", originalFilename, tempPath);

        return filePart.transferTo(tempPath)
                .then(validateAndSaveTemp(tempPath, metadata, nameNoExt, fileExtension, username))
                .then(metadataService.getMetadata(tempPath))
                .flatMap(meta -> transcodeAndUpload(meta, nameNoExt, fileExtension, tempPath, username, metadata, strategy))
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(ok -> tempFileCleanUpService.cleanupTempFile(tempPath).subscribe())
                .onErrorResume(ex -> {
                    log.error("Upload failed: {}", ex.getMessage(), ex);
                    return Mono.error(new FileUploadException("❌ Upload failed: " + ex.getMessage()));
                });
    }


    @Override
    @Transactional
    public Mono<ResponseEntity<String>> uploadFileAndTranscodeV1(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        // Temporarily enforce the storage strategy to be LOCAL directly
        StorageStrategy strategy = storageStrategyFactory.getStrategy(Storage.LOCAL.name());

        String username = auth.getName();
        String originalFilename = filePart.filename();
        String fileExtension = getExtension(originalFilename);
        String originalNameWithoutExtension = FilenameUtils.removeExtension(originalFilename);
        String tempSaveName = sanitizeFilenameWithoutExtension(originalNameWithoutExtension) + "." + fileExtension; // the originalFilename with extension has no postfix yet

        Path tempPath = Paths.get(tempDir, tempSaveName);
        log.info("Uploading file {} to temp directory {}", originalFilename, tempPath);

        return filePart.transferTo(tempPath) // store on temp folder
                .then(fileValidationService.validateFile(tempPath))
                .then(Mono.defer(() -> {
                    TempFileRecord tempFileRecord = createNewTempUploadEvent(metadata, originalNameWithoutExtension, fileExtension, tempPath, username);
                    return tempFileRecordRepository.saveTempFile(tempFileRecord);
                }))
                .then(metadataService.getMetadata(tempPath)) // probe before storing => get precise metadata
                .flatMap(videoMeta -> {
                    log.info("Metadata: {}", videoMeta);
                    log.info("Uploading file {} to original directory {}", originalFilename, tempPath);
                    List<VideoTranscoderService.VideoResolution> videoResolutionList =
                            getResolutionsToTranscode(videoMeta, originalNameWithoutExtension, getExtension(originalFilename));
                    log.info("Resolutions to transcode: {}", videoResolutionList.stream().toList().toString());

                    return transcoderService.transcode(tempPath, videoResolutionList, uploadDir)
                            .flatMap(paths -> {
                                log.info("Transcoded paths: {}", paths.stream().map(Path::toString).toList());
                                List<Mono<FileUploadEvent>> monos = new ArrayList<>();

                                for (int i = 0; i < paths.size(); i++) {
                                    Path transcoded = paths.get(i);
                                    log.info("Transcoded path: {}", transcoded);
                                    VideoTranscoderService.VideoResolution videoResolution = videoResolutionList.get(i);
                                    String resLabel;
                                    String[] parts = videoResolution.filename().split("_");
                                    if (parts.length < 2) {
                                        log.warn("Unexpected filename format: {}", videoResolution.filename());
                                        resLabel = "unknown";
                                    } else {
                                        resLabel = parts[1].replace(".mp4", "");
                                    }
                                    log.info("Transcoded path: {}, quality: {}", transcoded, resLabel);


                                    Mono<FileUploadEvent> mono = strategy.store(transcoded.toFile(), videoResolution.filename())
                                            .flatMap(result -> {
                                                log.info("Stored transcoded file {} as {}", videoResolution.filename(), result.getUrl());
                                                FileUploadEvent event = createEvent(videoResolution.filename(), username, metadata, result);
                                                event.setQuality(resLabel);
                                                return publishEventAndOutbox(event);
                                            });
                                    monos.add(mono);
                                    log.info("Executing Mono: {}", monos);
                                }

                                Flux.merge(monos)
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();

                                return Mono.just(ResponseEntity.ok("Video uploaded and processed."))
                                        .publishOn(Schedulers.boundedElastic())
                                        .doOnNext(response ->
                                                tempFileCleanUpService.cleanupTempFile(tempPath)
                                                .subscribe()
                                        );
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
    public Flux<String> uploadFileAndTranscodeWithProgressV2(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        // Temporarily enforce the storage strategy to be LOCAL directly
        StorageStrategy strategy = storageStrategyFactory.getStrategy(Storage.LOCAL.name());

        String username = auth.getName();
        String originalFilename = filePart.filename();
        log.info("Uploading file to original directory {}", originalFilename);
        String fileExtension = getExtension(originalFilename);
        log.info("File extension: {}", fileExtension);
        String originalNameWithoutExtension = sanitizeFilenameWithoutExtension(originalFilename);
        log.info("Original name without extension: {}", originalNameWithoutExtension);
        String tempSaveName = originalNameWithoutExtension + "." + fileExtension;
        log.info("Temp save name with extension: {}", tempSaveName);

        Path tempPath = Paths.get(tempDir, tempSaveName);

        return Flux.create(sink -> {
            sink.next("⏳ Starting upload: " + originalFilename);

            filePart.transferTo(tempPath) // save original file to temp folder
                    .then(fileValidationService.validateFile(tempPath))
                    .doOnNext(isValid -> {
                        if (Boolean.TRUE.equals(isValid)) {
                            sink.next("✅ Original video stored.");
                        } else {
                            sink.error(new UnsupportedExtension("Unsupported extension: " + fileExtension));
                            sink.complete();
                        }
                    })
                    .then(Mono.defer(() -> {
                        TempFileRecord newRecord = createNewTempUploadEvent(metadata, originalNameWithoutExtension, fileExtension, tempPath, username);
                        return tempFileRecordRepository.saveTempFile(newRecord);
                    }))
                    .then(metadataService.getMetadata(tempPath))
                    .flatMapMany(videoMeta -> {
                        sink.next("📦 Metadata probed: " + videoMeta.width() + "x" + videoMeta.height());
                        sink.next("Begin to transcode video into multiple resolutions.");

                        List<VideoTranscoderService.VideoResolution> videoResolutions =
                                getResolutionsToTranscode(videoMeta, originalNameWithoutExtension, getExtension(originalFilename));
                        log.info("Resolutions to transcode v2: {}", videoResolutions.stream().toList().toString());
                        return transcoderService.transcode(tempPath, videoResolutions, uploadDir)
                                .doOnNext(paths -> sink.next("🎞️ Transcoding complete for " + paths.size() + " resolutions"))
                                .flatMapMany(paths -> {
                                    List<Mono<FileUploadEvent>> events = new ArrayList<>();
                                    for (int i = 0; i < paths.size(); i++) {
                                        Path transcoded = paths.get(i);
                                        VideoTranscoderService.VideoResolution videoResolution = videoResolutions.get(i);
                                        String label = extractLabel(videoResolution.filename());
                                        log.info("{} | {}", videoResolution.filename(), label);

                                        events.add(strategy.store(transcoded.toFile(), videoResolution.filename())
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
                                    }

                                    if (events.isEmpty()) {
                                        sink.next("⚠️ No transcoded files were processed for storage.");
                                        return Flux.just("🎉 Upload complete (no transcoding performed).");
                                    }

                                    return Flux.merge(events)
                                            .map(event -> "📥 Published event for " + event.getTitle())
                                            .concatWith(Flux.just("🎉 Upload & processing complete."))
                                            .publishOn(Schedulers.boundedElastic())
                                            .doOnNext(response ->
                                                    tempFileCleanUpService.cleanupTempFile(tempPath).subscribe()
                                            );
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

    private String extractLabel(String filename) {
        String[] parts = filename.split("_");
        return parts.length >= 2 ? parts[1].replace(".mp4", "") : "unknown";
    }

    private Mono<ResponseEntity<String>> transcodeAndUpload(
            FFmpegVideoMetadata meta, String nameNoExt, String ext, Path tempPath,
            String username, UploadMetadata uploadMeta, StorageStrategy strategy
    ) {
        List<VideoTranscoderService.VideoResolution> resolutions =
                getResolutionsToTranscode(meta, nameNoExt, ext);

        return transcoderService.transcode(tempPath, resolutions, uploadDir)
                .flatMap(paths -> {
                    List<Mono<FileUploadEvent>> events = IntStream.range(0, paths.size())
                            .mapToObj(i -> handleTranscodedFile(
                                    paths.get(i),
                                    resolutions.get(i),
                                    username,
                                    uploadMeta,
                                    strategy
                            ))
                            .toList();

                    return Flux.merge(events)
                            .collectList()
                            .thenReturn(ResponseEntity.ok("🎉 Upload complete"));
                });
    }

    private Mono<FileUploadEvent> handleTranscodedFile(
            Path path,
            VideoTranscoderService.VideoResolution resolution,
            String username,
            UploadMetadata metadata,
            StorageStrategy strategy
    ) {
        String label = extractLabel(resolution.filename());

        return strategy.store(path.toFile(), resolution.filename())
                .doOnNext(result -> log.info("📤 Stored {} -> {}", resolution.filename(), result.getUrl()))
                .flatMap(result -> {
                    FileUploadEvent event = createEvent(resolution.filename(), username, metadata, result);
                    event.setQuality(label);
                    return publishEventAndOutbox(event);
                });
    }


    private Mono<Void> validateAndSaveTemp(Path tempPath, UploadMetadata meta, String nameNoExt, String ext, String username) {
        return fileValidationService.validateFile(tempPath)
                .then(Mono.defer(() -> {
                    TempFileRecord recordEntity = createNewTempUploadEvent(meta, nameNoExt, ext, tempPath, username);
                    return tempFileRecordRepository.saveTempFile(recordEntity).then();
                }));
    }


    private List<VideoTranscoderService.VideoResolution> getResolutionsToTranscode(
            FFmpegVideoMetadata meta, String baseNameWithoutExtension, String extension
    ) {
        return PREDEFINED_RESOLUTIONS.stream()
                .filter(def -> meta.width() >= def.minWidth())
                .map(def -> {
                    String filename = String.format("%s_%s.%s", baseNameWithoutExtension, def.suffix(), extension);
                    return new VideoTranscoderService.VideoResolution(def.targetWidth(), def.targetHeight(), filename);
                })
                .toList();
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