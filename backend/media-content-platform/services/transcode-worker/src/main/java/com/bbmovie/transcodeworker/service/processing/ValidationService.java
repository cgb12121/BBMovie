package com.bbmovie.transcodeworker.service.processing;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.validation.ClamAVService;
import com.bbmovie.transcodeworker.service.validation.TikaValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Composite validation service that combines:
 * - Tika: Content type validation
 * - ClamAV: Malware scanning
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final TikaValidationService tikaValidationService;
    private final ClamAVService clamAVService;

    /**
     * Validates a file for the given purpose.
     * Performs content type validation and malware scan.
     *
     * @param file    File to validate
     * @param purpose Upload purpose (determines allowed MIME types)
     * @throws RuntimeException if validation fails
     */
    public void validate(Path file, UploadPurpose purpose) {
        log.debug("Validating file: {} for purpose: {}", file, purpose);

        // 1. Content type validation
        tikaValidationService.validate(file, purpose);

        // 2. Malware scan
        clamAVService.scanFile(file);

        log.debug("Validation passed for: {}", file);
    }

    /**
     * Validates content type only (no malware scan).
     * Useful for quick validation during probing.
     */
    public void validateContentType(Path file, UploadPurpose purpose) {
        tikaValidationService.validate(file, purpose);
    }

    /**
     * Performs malware scan only.
     */
    public void scanForMalware(Path file) {
        clamAVService.scanFile(file);
    }
}

