package com.example.bbmovieuploadfile;

import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileUploadController {

    private static final Path UPLOAD_DIR = Paths.get("C:/Users/buith_ejh62q2/OneDrive/Tài liệu/GitHub/BBMovie/c2hvZXM-/bbmovie-upload-file/uploads");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(UPLOAD_DIR);
    }

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> handleUpload(
            @RequestPart("file") FilePart filePart,
            Authentication auth) {

        String username = auth.getName(); // From JWT "sub" claim
        String safeName = sanitizeFilename(filePart.filename());

        Path dest = UPLOAD_DIR.resolve(safeName).normalize();
        if (!dest.startsWith(UPLOAD_DIR)) {
            return Mono.just(ResponseEntity.badRequest().body("Invalid filename"));
        }

        return filePart.transferTo(dest)
                .thenReturn(ResponseEntity.ok("Uploaded by: " + username + ", file: " + safeName));
    }

    private String sanitizeFilename(String input) {
        return input.replaceAll("[^\\w\\-. ]", "_");
    }
}
