package com.example.bbmovie.dto.kafka.consumer;

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
    private Long movieId;
    private String fileType;       // "POSTER", "VIDEO", "TRAILER"
    private String url;            // Cloudinary, storage URL
    private String publicId;       // Cloudinary public ID
    private String quality;
    private String uploadedBy;
    private LocalDateTime timestamp;
}