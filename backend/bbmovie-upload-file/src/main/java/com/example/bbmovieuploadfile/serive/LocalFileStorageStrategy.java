package com.example.bbmovieuploadfile.serive;

import com.example.common.dtos.kafka.FileUploadResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component("localStorage")
public class LocalFileStorageStrategy implements FileStorageStrategy {

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
    public String getStorageType() {
        return "localStorage";
    }
}
