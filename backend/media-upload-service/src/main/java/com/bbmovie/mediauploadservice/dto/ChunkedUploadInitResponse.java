package com.bbmovie.mediauploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedUploadInitResponse {
    private String uploadId;
    private String objectKey;
    private Instant expiresAt;
    private Integer totalChunks;
    private Long chunkSizeBytes;
    private Long totalSizeBytes;
}

