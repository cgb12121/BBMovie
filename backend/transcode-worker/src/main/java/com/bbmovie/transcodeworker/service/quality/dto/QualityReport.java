package com.bbmovie.transcodeworker.service.quality.dto;

import java.time.Instant;

/**
 * VQS result for one rendition.
 */
public record QualityReport(
        String renditionSuffix,
        String metric,
        double score,
        Double psnrDb,
        Double ssimScore,
        boolean computed,
        String artifactUri,
        String note,
        Instant measuredAt
) {
    public static QualityReport skipped(String note) {
        return new QualityReport(
                "unknown",
                "VMAF",
                0.0,
                null,
                null,
                false,
                null,
                note,
                Instant.now()
        );
    }
}
