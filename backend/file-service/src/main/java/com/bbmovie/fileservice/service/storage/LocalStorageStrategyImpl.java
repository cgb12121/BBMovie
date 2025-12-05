package com.bbmovie.fileservice.service.storage;

import com.bbmovie.fileservice.exception.FileUploadException;
import com.bbmovie.common.dtos.nats.FileUploadResult;
import com.bbmovie.common.enums.Storage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Log4j2
@Component("localStorage")
public class LocalStorageStrategyImpl implements StorageStrategy {

    @Value("${spring.upload-dir}")
    private String uploadDir;

    @Override
    public Mono<FileUploadResult> store(File file, String filename) {
        return Mono.fromCallable(() -> {
            // Ensure directory exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // More robust creation
            }

            // Note: filename might contain subdirectories (e.g., attachments/xyz.pdf)
            // We need to resolve path properly
            Path destination = uploadPath.resolve(filename).normalize();

            // Ensure parent directory of destination exists
            Path parentDir = destination.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir); // More robust creation
            }

            try {
                Path path = Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                String contentType = Files.probeContentType(path);
                long fileSize = Files.size(path);

                // Use the relative path for database storage to avoid potential path length issues
                // The database path should be relative to the upload directory, not the absolute path
                String relativePath = Paths.get(uploadDir).relativize(path).toString();

                return new FileUploadResult(path.toAbsolutePath().toString(), relativePath, contentType, fileSize);
            } catch (IOException e) {
                log.error("Failed to store file: {}", filename, e);
                throw new FileUploadException("Failed to store file: " + filename);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> delete(String pathOrPublicId) {
        return Mono.fromRunnable(() -> {
            try {
                // If pathOrPublicId is absolute path
                Path path = Paths.get(pathOrPublicId);
                // If it's just filename, resolve against uploadDir
                if (!path.isAbsolute()) {
                    path = Paths.get(uploadDir).resolve(pathOrPublicId).normalize();
                }

                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Deleted local file: {}", path);
                } else {
                    log.warn("File not found for deletion: {}", path);
                }
            } catch (IOException e) {
                log.error("Failed to delete local file: {}", pathOrPublicId, e);
                // We might swallow exception or rethrow depending on if we want to fail the whole chain
                // Generally logging is enough for cleanup tasks
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public String getStorageType() {
        return Storage.LOCAL.name();
    }
}
