package com.bbmovie.transcodeworker.service.complexity;

import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phase-0 CAS placeholder.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.analysis.cas", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpComplexityAnalysisService implements ComplexityAnalysisService {

    private final boolean casEnabled;

    public NoOpComplexityAnalysisService(@Value("${app.analysis.cas.enabled:false}") boolean casEnabled) {
        this.casEnabled = casEnabled;
    }

    @Override
    public ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata) {
        if (casEnabled) {
            log.info("CAS is enabled but no concrete analyzer is registered yet for uploadId={}", uploadId);
        } else {
            log.debug("CAS disabled, returning baseline profile for uploadId={}", uploadId);
        }
        return ComplexityProfile.basic(uploadId);
    }
}
