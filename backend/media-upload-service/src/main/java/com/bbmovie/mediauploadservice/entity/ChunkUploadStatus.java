package com.bbmovie.mediauploadservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "chunk_upload_status",
    indexes = {
        @Index(name = "idx_chunk_upload_id_part", columnList = "uploadId,partNumber", unique = true),
        @Index(name = "idx_chunk_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkUploadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String uploadId;

    @Column(nullable = false)
    private Integer partNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChunkStatus status;

    private String etag;
    private String uploadUrl;
    private LocalDateTime urlExpiresAt;
    private Integer retryCount;
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime uploadedAt;

    public enum ChunkStatus {
        PENDING,      // Chunk chưa được upload
        UPLOADING,    // Đang upload
        UPLOADED,     // Upload thành công
        FAILED,       // Upload thất bại
        RETRYING      // Đang retry
    }
}

