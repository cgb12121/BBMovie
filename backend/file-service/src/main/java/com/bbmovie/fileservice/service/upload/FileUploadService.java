package com.bbmovie.fileservice.service.upload;

import com.bbmovie.fileservice.entity.FileAsset;
import com.bbmovie.fileservice.entity.cdc.OutboxFileRecord;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import com.bbmovie.fileservice.repository.TempFileRecordRepository;
import com.bbmovie.fileservice.service.concurrency.PrioritizedTaskExecutor;
import com.bbmovie.fileservice.service.concurrency.TaskPriority;
import com.bbmovie.fileservice.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.fileservice.service.ffmpeg.VideoMetadataService;
import com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.fileservice.service.nats.ReactiveFileUploadEventPublisher;
import com.bbmovie.fileservice.service.scheduled.TempFileCleanUpService;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import com.bbmovie.fileservice.service.storage.StorageStrategy;
import com.bbmovie.fileservice.service.validation.FileValidationService;
import com.example.common.dtos.nats.FileUploadEvent;
import com.example.common.dtos.nats.FileUploadResult;
import com.example.common.dtos.nats.UploadMetadata;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static com.bbmovie.fileservice.service.ffmpeg.VideoTranscoderService.PREDEFINED_RESOLUTIONS;
import static com.bbmovie.fileservice.utils.FileMetaDataUtils.sanitizeFilenameWithoutExtension;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createEvent;
import static com.bbmovie.fileservice.utils.FileUploadEventUtils.createNewTempUploadEvent;

@Log4j2
@Service
public class FileUploadService {

    @Value("${app.upload.temp-dir}")
    private String tempDir;

    @Value("${spring.upload-dir}")
    private String uploadDir;

    private final FileStorageStrategyFactory storageFactory;
    private final FileValidationService fileValidationService;
    private final TempFileRecordRepository tempFileRecordRepository;
    private final VideoMetadataService metadataService;
    private final VideoTranscoderService transcoderService;
    private final ReactiveFileUploadEventPublisher eventPublisher;
    private final TempFileCleanUpService tempFileCleanUpService;
    private final FileAssetRepository fileAssetRepository;
    private final PrioritizedTaskExecutor prioritizedTaskExecutor;

    @Autowired
    public FileUploadService(
            FileStorageStrategyFactory storageFactory,
            FileValidationService fileValidationService,
            TempFileRecordRepository tempFileRecordRepository,
            VideoMetadataService metadataService,
            VideoTranscoderService transcoderService,
            ReactiveFileUploadEventPublisher eventPublisher,
            TempFileCleanUpService tempFileCleanUpService,
            PrioritizedTaskExecutor prioritizedTaskExecutor,
            FileAssetRepository fileAssetRepository) {
        this.storageFactory = storageFactory;
        this.fileValidationService = fileValidationService;
        this.tempFileRecordRepository = tempFileRecordRepository;
        this.metadataService = metadataService;
        this.transcoderService = transcoderService;
        this.eventPublisher = eventPublisher;
        this.tempFileCleanUpService = tempFileCleanUpService;
        this.prioritizedTaskExecutor = prioritizedTaskExecutor;
        this.fileAssetRepository = fileAssetRepository;
    }

    public Mono<ResponseEntity<String>> orchestrateUpload(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String extension = FilenameUtils.getExtension(originalFilename);
        String nameNoExt = FilenameUtils.removeExtension(originalFilename);
        String tempSaveName = sanitizeFilenameWithoutExtension(nameNoExt) + "." + extension;
        Path tempPath = Paths.get(tempDir, tempSaveName);

        TaskPriority priority = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_MODERATOR"))
                ? TaskPriority.HIGH
                : TaskPriority.LOW;

        // The entire processing logic is now a single, flat, readable chain.
        Mono<Void> processingChain = filePart.transferTo(tempPath) // 1. Save to temp
                .then(fileValidationService.validateFile(tempPath)) // 2. Validate
                .then(Mono.defer(() -> { // 3. Save temp record
                    OutboxFileRecord tempRecord = createNewTempUploadEvent(metadata, nameNoExt, extension, tempPath, username);
                    return tempFileRecordRepository.saveTempFile(tempRecord);
                }))
                .then(Mono.defer(() -> metadataService.getMetadata(tempPath))) // 4. Get metadata
                .flatMap(videoMeta -> {
                    // 5. Determine resolutions and transcode
                    List<VideoTranscoderService.VideoResolution> resolutions = getResolutionsToTranscode(videoMeta, nameNoExt, extension);
                    return transcoderService.transcode(tempPath, resolutions, uploadDir)
                            .flatMapMany(Flux::fromIterable) // Stream of transcoded paths
                            .flatMap(transcodedPath -> {
                                // 6. For each path, store, save asset, and publish
                                StorageStrategy strategy = storageFactory.getStrategy(metadata.getStorage().name());
                                String qualityLabel = transcodedPath.getFileName().toString().split("_")[1].replace("." + extension, "");

                                return strategy.store(transcodedPath.toFile(), transcodedPath.getFileName().toString())
                                        .flatMap(uploadResult -> saveFileAsset(metadata, uploadResult, qualityLabel))
                                        .flatMap(savedAsset -> {
                                            FileUploadEvent event = createEvent(savedAsset.getPathOrPublicId(), username, metadata, new FileUploadResult(savedAsset.getPathOrPublicId(), savedAsset.getPathOrPublicId()));
                                            event.setQuality(savedAsset.getQuality());
                                            return eventPublisher.publish(event);
                                        });
                            }).then(); // Wait for all parallel inner tasks to complete
                });

        // Submit the entire chain to the prioritized executor
        Mono<Void> processingWithCleanup = processingChain
                .doOnError(error -> log.error("Processing failed for file {}: {}", originalFilename, error.getMessage()))
                .materialize()
                .flatMap(signal -> tempFileCleanUpService.cleanupTempFile(tempPath)
                        .onErrorResume(cleanupErr -> {
                            log.warn("Temp cleanup failed for {}: {}", originalFilename, cleanupErr.getMessage());
                            return Mono.empty();
                        })
                        .then(Mono.defer(() -> {
                            if (signal.isOnError()) {
                                return Mono.error(Objects.requireNonNull(signal.getThrowable()));
                            }
                            return Mono.empty();
                        })))
                .dematerialize();

        Scheduler prioritizedScheduler = Schedulers.fromExecutor(command -> prioritizedTaskExecutor.submit(command, priority));

        return processingWithCleanup
                .subscribeOn(prioritizedScheduler)
                .then(Mono.fromRunnable(() -> {}))
                .then(Mono.just(ResponseEntity.accepted().body("Upload accepted and is being processed.")));
    }

    private Mono<FileAsset> saveFileAsset(UploadMetadata metadata, FileUploadResult uploadResult, String qualityLabel) {
        try {
            FileAsset asset = FileAsset.builder()
                    .movieId(metadata.getMovieId())
                    .entityType(metadata.getEntityType())
                    .storageProvider(metadata.getStorage())
                    .pathOrPublicId(uploadResult.getPublicId())
                    .quality(qualityLabel)
                    .mimeType(uploadResult.getContentType())
                    .fileSize(uploadResult.getFileSize())
                    .build();
            return fileAssetRepository.insertFileAsset(asset).thenReturn(asset);
        } catch (Exception e) {
            return Mono.error(e);
        }
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
}