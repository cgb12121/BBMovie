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

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = ImageExtension.getAllowedExtensions()
            .stream()
            .map(ImageExtension::getExtension)
            .toList();

    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = VideoExtension.getAllowedVideoExtensions()
            .stream()
            .map(VideoExtension::getExtension)
            .toList();


    @Autowired
    public TikaValidationService(Tika tika) {
        this.tika = tika;
    }

    public Mono<Void> validateContentType(Path path) {
        return Mono.fromCallable(() -> {
            String contentType = tika.detect(path.toFile());
            String extension = contentType.substring(contentType.lastIndexOf("/") + 1).toLowerCase();
            log.info("Detected content type: {}, {}", contentType, extension);

            if (contentType.startsWith("image/") && ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                return true;
            }

            if (contentType.startsWith("video/") && ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
                return true;
            }

            log.error("Unsupported file type: {}", contentType);
            throw new UnsupportedExtension("Unsupported file type: " + contentType);
        }).then();
    }
}