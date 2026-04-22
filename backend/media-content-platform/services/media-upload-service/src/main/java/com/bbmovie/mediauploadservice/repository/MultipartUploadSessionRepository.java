package com.bbmovie.mediauploadservice.repository;

import com.bbmovie.mediauploadservice.entity.MultipartUploadSession;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MultipartUploadSessionRepository extends JpaRepository<@NonNull MultipartUploadSession, @NonNull UUID> {
    Optional<MultipartUploadSession> findByUploadId(String uploadId);
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}

