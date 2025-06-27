package com.example.bbmovieuploadfile.serive;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileUploadService {

    @Value("${spring.upload-dir}")
    private String uploadPath;
    private final Path uploadDir = Paths.get(uploadPath);

    private final FileUploadEventPublisher fileUploadEventPublisher;

    @Autowired
    public FileUploadService(FileUploadEventPublisher fileUploadEventPublisher) {
        this.fileUploadEventPublisher = fileUploadEventPublisher;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    public Mono<ResponseEntity<String>> uploadFile(FilePart filePart, Authentication auth) {
        String username = auth.getName(); // From Jose "sub" claim
        String safeName = sanitizeFilename(filePart.filename());

        Path dest = uploadDir.resolve(safeName).normalize();
        if (!dest.startsWith(uploadDir)) {
            return Mono.just(ResponseEntity.badRequest().body("Invalid filename"));
        }

        return filePart.transferTo(dest)
                .thenReturn(ResponseEntity.ok("Uploaded by: " + username + ", file: " + safeName));
    }

    private String sanitizeFilename(String input) {
        return input.replaceAll("[^\\w\\-. ]", "_");
    }
}
