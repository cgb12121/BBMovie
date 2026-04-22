package com.bbmovie.transcodeworker.service.quality;

import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Phase-0 VQS placeholder.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.analysis.vqs", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVideoQualityService implements VideoQualityService {

    private final boolean vqsEnabled;

    public NoOpVideoQualityService(@Value("${app.analysis.vqs.enabled:false}") boolean vqsEnabled) {
        this.vqsEnabled = vqsEnabled;
    }

    @Override
    public QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix) {
        if (vqsEnabled) {
            log.info("VQS is enabled but no concrete scorer is registered yet for file={}", encodedFile);
        } else {
            log.debug("VQS disabled, skip quality scoring for file={}", encodedFile);
        }
        return new QualityReport(
                renditionSuffix,
                "VMAF",
                0.0,
                null,
                null,
                false,
                null,
                "VQS phase-0 placeholder",
                java.time.Instant.now()
        );
    }
}
