package com.bbmovie.fileservice.service.validation.tika;

import com.bbmovie.fileservice.exception.UnsupportedExtension;
import com.bbmovie.fileservice.service.ffmpeg.ImageExtension;
import com.bbmovie.fileservice.service.ffmpeg.VideoExtension;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;

@Log4j2
@Service
public class TikaValidationService {

    private final Tika tika;

    @Autowired
    public TikaValidationService(Tika tika) {
        this.tika = tika;
    }

    public Mono<String> getAndValidateContentType(Path path) {
        return Mono.fromCallable(() -> {
            String extension = tika.detect(path.toFile());
            log.info("Detected content type: {}", extension);
            if (extension.startsWith("image/")) {
                List<String> allowedImageExtensions = ImageExtension.getAllowedExtensions()
                        .stream()
                        .map(Enum::toString)
                        .toList();
                if (allowedImageExtensions.contains(extension)) {
                    return extension;
                }
            }
            if (extension.startsWith("video/")) {
                List<String> allowedVideoExtension = VideoExtension.getAllowedVideoExtensions()
                        .stream()
                        .map(Enum::toString)
                        .toList();
                if (allowedVideoExtension.contains(extension)) {
                    return extension;
                }
            }
            throw new UnsupportedExtension("Unsupported file type: " + extension);
        });
    }
}
