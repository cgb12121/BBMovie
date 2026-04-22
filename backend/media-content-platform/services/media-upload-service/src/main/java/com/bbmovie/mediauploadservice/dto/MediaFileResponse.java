package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.MediaStatus;
import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MediaFileResponse {
    private String uploadId;
    private String originalFilename;
    private UploadPurpose purpose;
    private MediaStatus status;
    private Long sizeBytes;
    private String mimeType;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime uploadedAt;
}
