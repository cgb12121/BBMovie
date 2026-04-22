package com.bbmovie.transcodeworker.service.validation;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.exception.UnsupportedExtension;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Service class for validating file content types using Apache Tika.
 * Determines the actual content type of uploaded files and checks it against allowed types for the upload purpose.
 * <p>
 * Supports both file-based and stream-based validation for flexibility
 * in different processing scenarios.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TikaValidationService {

    /** Apache Tika instance used for content type detection */
    private final Tika tika;

    /**
     * Validates the content type of the file against the allowed types for the specified upload purpose.
     * This method detects the actual content type of the file (not relying on extension) and ensures
     * it matches the allowed MIME types for the given purpose.
     *
     * @param path the path to the file to validate
     * @param purpose the upload purpose that determines which content types are allowed
     * @throws UnsupportedExtension if the detected content type is not allowed for the specified purpose
     */
    public void validate(Path path, UploadPurpose purpose) {
        try {
            String detectedContentType = tika.detect(path.toFile());
            validateContentType(detectedContentType, purpose);
        } catch (UnsupportedExtension e) {
            throw e;
        } catch (Exception e) {
            log.error("Validation failed for file: {}", path, e);
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the content type from an InputStream.
     * <p>
     * This allows validation without downloading the entire file,
     * useful for stream-based processing in the pipeline.
     * <p>
     * Note: The stream will be read for content type detection.
     * Make sure to use a buffered stream or mark/reset if you need
     * to read the content afterward.
     *
     * @param inputStream the input stream to validate
     * @param purpose the upload purpose that determines which content types are allowed
     * @throws UnsupportedExtension if the detected content type is not allowed for the specified purpose
     */
    public void validate(InputStream inputStream, UploadPurpose purpose) {
        try {
            String detectedContentType = tika.detect(inputStream);
            validateContentType(detectedContentType, purpose);
        } catch (UnsupportedExtension e) {
            throw e;
        } catch (Exception e) {
            log.error("Validation failed for stream", e);
            throw new RuntimeException("Validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Detects content type from a file without validation.
     *
     * @param path the path to the file
     * @return detected content type
     */
    public String detectContentType(Path path) {
        try {
            return tika.detect(path.toFile());
        } catch (Exception e) {
            log.error("Failed to detect content type for file: {}", path, e);
            throw new RuntimeException("Content type detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Detects content type from an InputStream without validation.
     *
     * @param inputStream the input stream
     * @return detected content type
     */
    public String detectContentType(InputStream inputStream) {
        try {
            return tika.detect(inputStream);
        } catch (Exception e) {
            log.error("Failed to detect content type from stream", e);
            throw new RuntimeException("Content type detection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to validate a detected content type against allowed types.
     *
     * @param detectedContentType the detected content type
     * @param purpose the upload purpose
     * @throws UnsupportedExtension if the content type is not allowed
     */
    private void validateContentType(String detectedContentType, UploadPurpose purpose) {
        log.info("Detected content type: {} for purpose: {}", detectedContentType, purpose);

        if (!purpose.getAllowedMimeTypes().contains(detectedContentType)) {
            log.error("Unsupported file type: {} for purpose {}", detectedContentType, purpose);
            throw new UnsupportedExtension("File type " + detectedContentType + " is not allowed for " + purpose);
        }
    }
}
