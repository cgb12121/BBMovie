package com.bbmovie.transcodeworker.service.validation;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.exception.UnsupportedExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Log4j2
@Service
@RequiredArgsConstructor
public class TikaValidationService {

    private final Tika tika;

    public void validate(Path path, UploadPurpose purpose) {
        try {
            String detectedContentType = tika.detect(path.toFile());
            log.info("Detected content type: {} for purpose: {}", detectedContentType, purpose);

            if (!purpose.getAllowedMimeTypes().contains(detectedContentType)) {
                log.error("Unsupported file type: {} for purpose {}", detectedContentType, purpose);
                throw new UnsupportedExtension("File type " + detectedContentType + " is not allowed for " + purpose);
            }
        } catch (Exception e) {
            log.error("Validation failed for file: {}", path, e);
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        }
    }
}
