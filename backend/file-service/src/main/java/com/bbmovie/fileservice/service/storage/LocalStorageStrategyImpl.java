package com.bbmovie.fileservice.service.storage;

import com.bbmovie.fileservice.exception.FileUploadException;
import com.example.common.dtos.nats.FileUploadResult;
import com.example.common.enums.Storage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
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
    public Mono<FileUploadResult> store(FilePart filePart, String safeName) {
        Path dest = Paths.get(uploadDir).resolve(safeName).normalize();

        if (!dest.startsWith(Paths.get(uploadDir))) {
            return Mono.error(new SecurityException("Invalid filename"));
        }

        return filePart.transferTo(dest).flatMap(v -> {
            try {
                String contentType = Files.probeContentType(dest);
                long fileSize = Files.size(dest);
                return Mono.just(new FileUploadResult(dest.toUri().toString(), safeName, contentType, fileSize));
            } catch (IOException e) {
                return Mono.error(new FileUploadException("Failed to read file attributes after storing: " + safeName, e));
            }
        });
    }

    @Override
    public Mono<FileUploadResult> store(File file, String filename) {
        return Mono.fromCallable(() -> {
            Path destination = Paths.get(uploadDir, filename);
            try {
                Path path = Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                String url = path.toFile().getAbsolutePath();
                String contentType = Files.probeContentType(path);
                long fileSize = Files.size(path);
                return new FileUploadResult(url, filename, contentType, fileSize);
            } catch (IOException e) {
                log.error("Failed to store file: {}", filename, e);
                throw new FileUploadException("Failed to store file: " + filename);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getStorageType() {
        return Storage.LOCAL.name();
    }
}
