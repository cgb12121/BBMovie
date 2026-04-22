package com.bbmovie.mediauploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadProgressResponse {
    private String uploadId;
    private Integer totalChunks;
    private Integer uploadedChunks;
    private Integer failedChunks;
    private Integer pendingChunks;
    private Double progressPercentage;
    private Map<Integer, String> chunkStatuses; // partNumber -> status
}

