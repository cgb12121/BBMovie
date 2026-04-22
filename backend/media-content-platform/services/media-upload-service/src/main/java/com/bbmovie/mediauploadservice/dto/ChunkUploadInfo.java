package com.bbmovie.mediauploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadInfo {
    private Integer partNumber;
    private String uploadUrl;
    private Long startByte;
    private Long endByte;
}

