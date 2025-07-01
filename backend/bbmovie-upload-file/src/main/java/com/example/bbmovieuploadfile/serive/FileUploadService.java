package com.example.bbmovieuploadfile.serive;

import com.example.common.dtos.kafka.FileUploadResult;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.dtos.kafka.FileUploadEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class FileUploadService {

    private final FileUploadEventPublisher fileUploadEventPublisher;
    private final FileStorageStrategyFactory storageStrategyFactory;

    @Autowired
    public FileUploadService(
            FileUploadEventPublisher fileUploadEventPublisher,
            FileStorageStrategyFactory storageStrategyFactory
    ) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
        this.storageStrategyFactory = storageStrategyFactory;
    }

    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, UploadMetadata metadata, Authentication auth) {
        String username = auth.getName();
        String originalFilename = filePart.filename();
        String safeName = sanitizeFilename(originalFilename);

        FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

        return strategy.store(filePart, safeName)
                .flatMap(result -> {
                    FileUploadEvent event = createEvent(safeName, username, metadata, result);
                    fileUploadEventPublisher.publish(event);
                    return Mono.just(ResponseEntity.ok("Uploaded: " + result.getUrl()));
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body("Upload failed: " + ex.getMessage())));
    }

    private String sanitizeFilename(String input) {
        int lastDotIndex = input.lastIndexOf('.');
        String fileNameWithoutExtension;
        String fileExtension = "";

        if (lastDotIndex > 0) {
            fileNameWithoutExtension = input.substring(0, lastDotIndex);
            fileExtension = input.substring(lastDotIndex);
        } else {
            fileNameWithoutExtension = input;
        }

        String sanitizedFileName = fileNameWithoutExtension.replaceAll("[^\\w\\- ]", "_");

        return sanitizedFileName + fileExtension;
    }

    private FileUploadEvent createEvent(@NonNull String fileName, @NonNull String uploader,
            @NonNull UploadMetadata metadata, @NonNull FileUploadResult result
    ) {
        return FileUploadEvent.builder()
                .title(fileName)
                .fileType(metadata.getFileType().name())
                .url(result.getUrl())
                .publicId(result.getPublicId())
                .quality(metadata.getQuality())
                .uploadedBy(uploader)
                .timestamp(LocalDateTime.now())
                .build();
    }
}