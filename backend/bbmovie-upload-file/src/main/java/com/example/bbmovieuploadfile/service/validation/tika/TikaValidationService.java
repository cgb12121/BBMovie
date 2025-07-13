package com.example.bbmovieuploadfile.service.validation.tika;

import com.example.bbmovieuploadfile.exception.UnsupportedExtension;
import com.example.bbmovieuploadfile.service.ffmpeg.ImageExtension;
import com.example.bbmovieuploadfile.service.ffmpeg.VideoExtension;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

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
