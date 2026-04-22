package com.bbmovie.mediauploadservice.entity;

import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.enums.StorageProvider;
import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "media_files",
    indexes = {
        @Index(name = "idx_media_upload_id", columnList = "uploadId"),
        @Index(name = "idx_media_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ID generated before presign
     * Used to correlate MinIO / NATS events
     */
    @Column(nullable = false, unique = true, updatable = false)
    private String uploadId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageProvider storageProvider;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaStatus status;

    private Long sizeBytes;
    
    @Column(length = 100)
    private String mimeType;

    /**
     * SHA-256 to detect duplicate / integrity
     */
    private String checksum;
    
    /**
     * Hash of sampled chunks (Start, Middle, End) and file size for fast lookup
     */
    @Column(name = "sparse_checksum")
    private String sparseChecksum;
    
    @Column(length = 1024)
    private String rejectReason;

    @Column(name = "duplicate_of_id")
    private UUID duplicateOfId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime uploadedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime rejectedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}