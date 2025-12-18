package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.dto.MediaStatusUpdateEvent;
import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.repository.MediaFileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaStatusUpdateListener {

    private final Connection natsConnection;
    private final MediaFileRepository mediaFileRepository;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe("media.status.update");
    }

    private void handleMessage(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            MediaStatusUpdateEvent event = objectMapper.readValue(json, MediaStatusUpdateEvent.class);
            log.info("Received status update for uploadId: {}, status: {}", event.getUploadId(), event.getStatus());

            Optional<MediaFile> mediaFileOpt = mediaFileRepository.findByUploadId(event.getUploadId());
            if (mediaFileOpt.isPresent()) {
                MediaFile file = mediaFileOpt.get();
                file.setStatus(event.getStatus());
                
                if (event.getChecksum() != null) {
                    file.setChecksum(event.getChecksum());
                }
                if (event.getFileSize() != null) {
                    file.setSizeBytes(event.getFileSize());
                }
                if (event.getSparseChecksum() != null) {
                    file.setSparseChecksum(event.getSparseChecksum());
                }

                if (event.getStatus() == MediaStatus.VALIDATED) { // VALIDATED implies hashing is done by worker
                    // First, try to find a duplicate using the sparse checksum for a quick check
                    if (event.getSparseChecksum() != null) {
                        mediaFileRepository.findFirstBySparseChecksumAndStatus(event.getSparseChecksum(), MediaStatus.READY)
                                .ifPresent(sparseOriginal -> {
                                    log.info("Potential duplicate (sparse checksum) found! Current uploadId: {} matched sparse checksum of MediaFile: {}", 
                                            event.getUploadId(), sparseOriginal.getId());
                                    file.setDuplicateOfId(sparseOriginal.getId());
                                    // At this point, the worker is supposed to then do the full hash check
                                    // and send another event or update more thoroughly.
                                    // For now, we only mark it as potential duplicate.
                                });
                    }
                    
                    // If full checksum is available and different from sparse one, check for full duplicate
                    if (event.getChecksum() != null) {
                        mediaFileRepository.findFirstByChecksumAndStatus(event.getChecksum(), MediaStatus.READY)
                                .ifPresent(fullOriginal -> {
                                    log.info("Duplicate file (full checksum) detected! UploadId: {} is a duplicate of MediaFile: {}", 
                                            event.getUploadId(), fullOriginal.getId());
                                    file.setDuplicateOfId(fullOriginal.getId());
                                    // Here, we know it's a definite duplicate.
                                    // If the worker sends VALIDATED with full hash, and it's a duplicate,
                                    // the upload service can then decide to:
                                    // - delete the raw file uploaded by current user
                                    // - set current file status to READY
                                    // - link to existing variants (implementation detail for later)
                                });
                    }
                }

                if (event.getStatus() == MediaStatus.REJECTED) {
                    file.setRejectReason(event.getReason() != null ? event.getReason() : "Rejected by system");
                    
                    // Immediately delete from MinIO
                    try {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                        .bucket(file.getBucket())
                                        .object(file.getObjectKey())
                                        .build()
                        );
                        log.info("Deleted rejected file from storage: {}", file.getObjectKey());
                    } catch (Exception e) {
                        log.error("Failed to delete rejected file from storage: {}", file.getObjectKey(), e);
                    }
                } else if (event.getStatus() == MediaStatus.FAILED) {
                     file.setRejectReason(event.getReason() != null ? event.getReason() : "Processing failed");
                }

                mediaFileRepository.save(file);
            } else {
                log.warn("MediaFile not found for uploadId: {}", event.getUploadId());
            }

        } catch (Throwable e) {
            log.error("Error processing NATS message", e);
        }
    }
}
