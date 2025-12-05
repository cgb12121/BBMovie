package com.bbmovie.fileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUploadResponse {
    private Long fileId;
    private String storageType;
    private String path; // Relative or Absolute path, or Public ID
    private String accessUrl; // URL to download/access the file
}
