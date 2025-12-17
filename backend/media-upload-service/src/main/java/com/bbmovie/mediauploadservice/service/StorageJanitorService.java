package com.bbmovie.mediauploadservice.service;

import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.repository.MediaFileRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageJanitorService {

    private final MinioClient minioClient;
    private final MediaFileRepository mediaFileRepository;

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupOrphanedFiles() {
        LocalDateTime cutOff = LocalDateTime.now().minusHours(24); // More conservative 24h for general cleanup
        
        // Clean up files that are stuck in INITIATED (presign expired) or UPLOADED (processing stuck)
        List<MediaFile> orphans = mediaFileRepository.findAllByStatusInAndCreatedAtBefore(
                List.of(MediaStatus.INITIATED),
                cutOff
        );

        for (MediaFile file : orphans) {
            try {
                // Remove file from MinIO
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(file.getBucket())
                        .object(file.getObjectKey())
                        .build()
                );

                // Update status to EXPIRED
                file.setStatus(MediaStatus.EXPIRED);
                
                if (file.getStatus() == MediaStatus.INITIATED) {
                    file.setRejectReason("Upload expired (Presigned URL timeout)");
                } else {
                    file.setRejectReason("Processing timeout (Stuck in UPLOADED state)");
                }
                
                mediaFileRepository.save(file);

                log.info("Cleaned up orphan file: {}", file.getId());
            } catch (Exception e) {
                log.error("Failed to clean file {}", file.getId(), e);
            }
        }
    }
}
