package com.example.bbmovieuploadfile.utils;

import com.example.common.dtos.kafka.FileUploadEvent;
import com.example.common.dtos.kafka.FileUploadResult;
import com.example.common.dtos.kafka.UploadMetadata;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;

public class FileUploadEventUtils {
    private FileUploadEventUtils() { }

    public static FileUploadEvent createEvent(
            @NonNull String fileName, @NonNull String uploader,
            @NonNull UploadMetadata metadata, @NonNull FileUploadResult result
    ) {
        return FileUploadEvent.builder()
                .title(fileName)
                .entityType(metadata.getEntityType())
                .storage(metadata.getStorage())
                .url(result.getUrl())
                .publicId(result.getPublicId())
                .quality(metadata.getQuality())
                .uploadedBy(uploader)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
