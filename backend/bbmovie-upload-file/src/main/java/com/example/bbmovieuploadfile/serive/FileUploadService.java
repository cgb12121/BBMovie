package com.example.bbmovieuploadfile.serive;

import com.example.bbmovieuploadfile.dto.FileUploadEvent;
import com.example.bbmovieuploadfile.dto.UploadMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
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
        String safeName = sanitizeFilename(filePart.filename());

        FileStorageStrategy strategy = storageStrategyFactory.getStrategy(metadata.getStorage().name());

        return strategy.store(filePart, safeName)
                .flatMap(result -> {
                    FileUploadEvent event = FileUploadEvent.builder()
                            .name(safeName)
                            .fileType(metadata.getFileType().name())
                            .url(result.getUrl())
                            .publicId(result.getPublicId())
                            .quality(metadata.getQuality())
                            .uploadedBy(username)
                            .timestamp(LocalDateTime.now())
                            .build();
                    fileUploadEventPublisher.publish(event);
                    return Mono.just(ResponseEntity.ok("Uploaded: " + result.getUrl()));
                })
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body("Upload failed: " + ex.getMessage())));
    }

    private String sanitizeFilename(String input) {
        return input.replaceAll("[^\\w\\-. ]", "_").replaceAll("[./\\\\]", "");
    }
}