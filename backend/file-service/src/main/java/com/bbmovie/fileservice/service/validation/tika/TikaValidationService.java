package com.bbmovie.fileservice.service.validation.tika;

import com.bbmovie.fileservice.exception.UnsupportedExtension;
import com.bbmovie.fileservice.utils.FileTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;

@Log4j2
@Service
@RequiredArgsConstructor
public class TikaValidationService {

    private final Tika tika;


    public Mono<String> getContentType(Path path) {
        return Mono.fromCallable(() -> {
            String contentType = tika.detect(path.toFile());
            String fileName = path.getFileName().toString().toLowerCase();
            String fileExtension = FileTypeUtils.getFileExtension(fileName);

            log.info("Detected content type: {}", contentType);

            return contentType;
        });
    }

    public Mono<Void> validateContentType(Path path) {
        return Mono.fromCallable(() -> {
            String contentType = tika.detect(path.toFile());
            String fileName = path.getFileName().toString().toLowerCase();
            String fileExtension = FileTypeUtils.getFileExtension(fileName);

            log.info("Detected content type: {}, File extension: {}", contentType, fileExtension);

            // Check image files
            if (contentType.startsWith("image/") && FileTypeUtils.getAllowedImageExtensions().contains(fileExtension)) {
                return true;
            }

            // Check video files
            if (contentType.startsWith("video/") && FileTypeUtils.getAllowedVideoExtensions().contains(fileExtension)) {
                return true;
            }

            // Check PDF files
            if (contentType.contains("/pdf") && FileTypeUtils.getAllowedPdfExtensions().contains(fileExtension)) {
                return true;
            }

            // Check audio files - Tika may detect these as audio/mpeg, audio/wav, etc.
            if (contentType.startsWith("audio/") && FileTypeUtils.getAllowedAudioExtensions().contains(fileExtension)) {
                return true;
            }

            // Check text files - Tika may detect these as text/plain, application/json, etc.
            if (FileTypeUtils.isTextFile(contentType, fileExtension)) {
                return true;
            }

            log.error("Unsupported file type: {} (extension: {})", contentType, fileExtension);
            throw new UnsupportedExtension("Unsupported file type: " + contentType + " (extension: " + fileExtension + ")");
        }).then();
    }
}