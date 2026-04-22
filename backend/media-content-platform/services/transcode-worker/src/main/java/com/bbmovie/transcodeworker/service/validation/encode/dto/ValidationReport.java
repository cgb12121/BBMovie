package com.bbmovie.transcodeworker.service.validation.encode.dto;

import java.time.Instant;
import java.util.List;

/**
 * VVS validation result for one encoded output.
 */
public record ValidationReport(
        ValidationStatus status,
        List<String> violations,
        String artifactUri,
        Instant validatedAt
) {
    public static ValidationReport skipped(String reason) {
        return new ValidationReport(
                ValidationStatus.SKIPPED,
                List.of(reason),
                null,
                Instant.now()
        );
    }

    public enum ValidationStatus {
        PASS,
        WARN,
        FAIL,
        SKIPPED
    }
}
