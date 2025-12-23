package com.bbmovie.mediauploadservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "multipart_upload_sessions",
    indexes = {
        @Index(name = "idx_multipart_upload_id", columnList = "uploadId"),
        @Index(name = "idx_multipart_expires_at", columnList = "expiresAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultipartUploadSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String uploadId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String minioUploadId; // MinIO multipart upload ID

    @Column(nullable = false)
    private Integer totalChunks;

    @Column(nullable = false)
    private Long totalSizeBytes;

    @Column(nullable = false)
    private Long chunkSizeBytes;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean completed = false;
}

