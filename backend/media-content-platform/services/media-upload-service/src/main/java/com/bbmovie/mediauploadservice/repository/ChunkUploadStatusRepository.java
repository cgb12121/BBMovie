package com.bbmovie.mediauploadservice.repository;

import com.bbmovie.mediauploadservice.entity.ChunkUploadStatus;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChunkUploadStatusRepository extends JpaRepository<@NonNull ChunkUploadStatus, @NonNull UUID> {
    Optional<ChunkUploadStatus> findByUploadIdAndPartNumber(String uploadId, Integer partNumber);
    List<ChunkUploadStatus> findByUploadIdOrderByPartNumberAsc(String uploadId);
    List<ChunkUploadStatus> findByUploadIdAndPartNumberBetweenOrderByPartNumberAsc(
        String uploadId, Integer fromPart, Integer toPart);
    
    @Query("SELECT COUNT(c) FROM ChunkUploadStatus c WHERE c.uploadId = :uploadId AND c.status = :status")
    long countByUploadIdAndStatus(@Param("uploadId") String uploadId, 
                                   @Param("status") ChunkUploadStatus.ChunkStatus status);
    
    @Modifying
    @Query("DELETE FROM ChunkUploadStatus c WHERE c.uploadId = :uploadId")
    void deleteByUploadId(@Param("uploadId") String uploadId);
}

