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
public class UploadInitRequest {
    @NotNull(message = "Purpose is required")
    private UploadPurpose purpose;

    @NotNull(message = "Content type is required")
    private String contentType;

    private Long sizeBytes;
    private String filename;
    private String checksum;
    private String sparseChecksum;
}
