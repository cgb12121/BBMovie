package com.bbmovie.fileservice.utils;

import com.bbmovie.fileservice.entity.cdc.OutboxFileRecord;
import com.bbmovie.common.dtos.nats.FileUploadEvent;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.bbmovie.common.dtos.nats.UploadMetadata;
import org.springframework.lang.NonNull;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class FileUploadEventUtils {
    private FileUploadEventUtils() { }

    public static FileUploadEvent createEvent(
            @NonNull String fileName, @NonNull String uploader,
            @NonNull UploadMetadata metadata, @NonNull FileUploadResult result
    ) {
        return FileUploadEvent.builder()
                .title(fileName)
                .entityType(metadata.getFileType())
                .storage(metadata.getStorage())
                .url(result.getUrl())
                .publicId(result.getPublicId())
                .quality(metadata.getQuality())
                .uploadedBy(uploader)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static OutboxFileRecord createNewTempUploadEvent(
            UploadMetadata metadata, String originalNameWithoutExtension,
            String fileExtension, Path tempPath, String username
    ) {
        return OutboxFileRecord.builder()
                .id(UUID.randomUUID().toString())
                .fileName(originalNameWithoutExtension)
                .extension(fileExtension)
                .tempDir(tempPath.toString())
                .tempStoreFor(metadata.getFileType().name())
                .uploadedBy(username)
                .isRemoved(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
