package com.bbmovie.mediauploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkBatchResponse {
    private String uploadId;
    private Integer totalChunks;
    private Integer fromPart;
    private Integer toPart;
    private List<ChunkUploadInfo> chunks;
}

