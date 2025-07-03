package com.example.common.dtos.kafka;

import com.example.common.enums.EntityType;
import com.example.common.enums.Storage;
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
    private String title;
    private EntityType entityType;
    private Storage storage;
    private String url;
    private String publicId;
    private String quality;
    private String uploadedBy;
    private LocalDateTime timestamp;
}