package com.bbmovie.mediauploadservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteChunkedUploadRequest {
    @NotNull(message = "Upload ID is required")
    private String uploadId;

    // Parts are optional - if not provided, backend will get them from chunk tracking
    private List<PartInfo> parts;
}

