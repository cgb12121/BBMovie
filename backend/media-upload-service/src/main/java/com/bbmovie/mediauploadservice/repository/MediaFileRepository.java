package com.bbmovie.mediauploadservice.repository;

import com.bbmovie.mediauploadservice.entity.MediaFile;
import com.bbmovie.mediauploadservice.enums.MediaStatus;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends
        JpaRepository<@NonNull MediaFile, @NonNull UUID>, JpaSpecificationExecutor<@NonNull MediaFile> {
    Optional<MediaFile> findByUploadId(String uploadId);
    List<MediaFile> findAllByStatusInAndCreatedAtBefore(List<MediaStatus> statuses, LocalDateTime dateTime);
    Optional<MediaFile> findFirstByChecksumAndStatus(String checksum, MediaStatus status);
    Optional<MediaFile> findFirstBySparseChecksumAndStatus(String sparseChecksum, MediaStatus status);
}