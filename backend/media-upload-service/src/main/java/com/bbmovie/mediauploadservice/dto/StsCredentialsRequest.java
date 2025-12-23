package com.bbmovie.mediauploadservice.dto;

import com.bbmovie.mediauploadservice.enums.UploadPurpose;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StsCredentialsRequest {
    @NotNull(message = "Purpose is required")
    private UploadPurpose purpose;

    private String filename;
    private Long sizeBytes;
    private String contentType;
    
    // Duration in seconds (default 1 hour)
    private Integer durationSeconds = 3600;
}

