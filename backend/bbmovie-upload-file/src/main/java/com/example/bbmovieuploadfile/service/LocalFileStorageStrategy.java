package com.example.bbmovieuploadfile.service;

import com.example.bbmovieuploadfile.exception.FileUploadException;
import com.example.common.dtos.kafka.FileUploadResult;
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
public class LocalFileStorageStrategy implements FileLocalStorageStrategy {

    @Value("${spring.upload-dir}")
    private String uploadDir;

    @Override
    public Mono<FileUploadResult> store(FilePart filePart, String safeName) {
        Path dest = Paths.get(uploadDir).resolve(safeName).normalize();

        if (!dest.startsWith(Paths.get(uploadDir))) {
            return Mono.error(new SecurityException("Invalid filename"));
        }

        return filePart.transferTo(dest).thenReturn(
                new FileUploadResult(dest.toUri().toString(), safeName)
        );
    }

    @Override
    public Mono<FileUploadResult> store(File file, String filename) {
        return Mono.fromCallable(() -> {
            Path destination = Paths.get(uploadDir, filename);
            try {
                Path path = Files.copy(file.toPath(), destination, StandardCopyOption.COPY_ATTRIBUTES);
                String url = path.toFile().getAbsolutePath();
                return new FileUploadResult(url, filename);
            } catch (IOException e) {
                log.error("Failed to store file: {}", filename, e);
                throw new FileUploadException("Failed to store file: " + filename);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getStorageType() {
        return "localStorage";
    }
}
