package com.bbmovie.fileservice.service.scheduled;

import com.bbmovie.common.enums.EntityType;
import com.bbmovie.fileservice.entity.FileStatus;
import com.bbmovie.fileservice.repository.FileAssetRepository;
import com.bbmovie.fileservice.service.storage.FileStorageStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileCleanupService {

    private final FileAssetRepository fileRepository;
    private final FileStorageStrategyFactory storageFactory;

    @Scheduled(fixedRate = 3600000) // Run every 1 hour
    public void cleanupOrphanedFiles() {
        log.info("Starting orphaned file cleanup...");
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        fileRepository.findByStatusAndCreatedAtBefore(FileStatus.PENDING, oneHourAgo)
                .filter(file -> {
                    // CRITICAL SAFETY CHECK: Never delete MOVIE or TRAILER files via this cronjob
                    // even if they somehow ended up as PENDING.
                    if (file.getEntityType() == EntityType.MOVIE || file.getEntityType() == EntityType.TRAILER) {
                        log.warn("Skipping cleanup for Video Entity: ID={}, Type={}", file.getId(), file.getEntityType());
                        return false;
                    }
                    return true;
                })
                .flatMap(file -> {
                    log.info("Deleting orphaned file: ID={}, Path={}, Type={}", file.getId(), file.getPathOrPublicId(), file.getEntityType());
                    // 1. Delete from Storage (MinIO/Local)
                    return storageFactory.getStrategy(file.getStorageProvider().name())
                            .delete(file.getPathOrPublicId())
                            .then(fileRepository.delete(file)) // 2. Delete from DB
                            .doOnError(e -> log.error("Failed to delete file {}: {}", file.getId(), e.getMessage()));
                })
                .subscribe();
    }
}