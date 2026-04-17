package com.bbmovie.transcodeworker.port.inprocess;

import com.bbmovie.transcodeworker.port.EncodeValidationPort;
import com.bbmovie.transcodeworker.service.validation.encode.EncodeValidationService;
import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InProcessEncodeValidationAdapter implements EncodeValidationPort {

    private final EncodeValidationService encodeValidationService;

    @Override
    public ValidationReport validate(
            String uploadId,
            Path encodedFile,
            String renditionSuffix,
            EncodingExpectations expectations) {
        return encodeValidationService.validate(uploadId, encodedFile, renditionSuffix, expectations);
    }
}
