package com.example.bbmovieuploadfile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadEvent {
    private String name;
    private String fileType;       // "POSTER", "VIDEO", "TRAILER"
    private String url;            // Cloudinary, storage URL
    private String publicId;       // Cloudinary public ID
    private String quality;        // "720P", "1080P"
    private String uploadedBy;
    private LocalDateTime timestamp;
}