package com.bbmovie.transcodeworker.service.validation.encode;

import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Phase-0 VVS placeholder.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.analysis.vvs", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpEncodeValidationService implements EncodeValidationService {

    private final boolean vvsEnabled;

    public NoOpEncodeValidationService(@Value("${app.analysis.vvs.enabled:false}") boolean vvsEnabled) {
        this.vvsEnabled = vvsEnabled;
    }

    @Override
    public ValidationReport validate(
            String uploadId,
            Path encodedFile,
            String renditionSuffix,
            EncodingExpectations expectations) {
        if (vvsEnabled) {
            log.info("VVS is enabled but no concrete validator is registered yet for file={}", encodedFile);
        } else {
            log.debug("VVS disabled, skip validation for file={}", encodedFile);
        }
        return ValidationReport.skipped("VVS phase-0 placeholder");
    }
}
