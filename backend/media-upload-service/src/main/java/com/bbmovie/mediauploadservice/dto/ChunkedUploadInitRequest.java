package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkedUploadInitRequest {
    @NotNull(message = "Purpose is required")
    private UploadPurpose purpose;

    @NotNull(message = "Content type is required")
    private String contentType;

    @NotNull(message = "Total size is required")
    @Positive(message = "Total size must be positive")
    private Long totalSizeBytes;

    @NotNull(message = "Chunk size is required")
    @Positive(message = "Chunk size must be positive")
    private Long chunkSizeBytes;

    @NotNull(message = "Total chunks is required")
    @Positive(message = "Total chunks must be positive")
    private Integer totalChunks;

    private String filename;
    private String checksum;
    private String sparseChecksum;
}

