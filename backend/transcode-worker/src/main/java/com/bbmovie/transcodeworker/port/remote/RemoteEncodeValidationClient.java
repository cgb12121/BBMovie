package com.bbmovie.transcodeworker.port.remote;

import com.bbmovie.transcodeworker.port.EncodeValidationPort;
import com.bbmovie.transcodeworker.service.validation.encode.dto.EncodingExpectations;
import com.bbmovie.transcodeworker.service.validation.encode.dto.ValidationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "true")
public class RemoteEncodeValidationClient implements EncodeValidationPort {

    @Override
    public ValidationReport validate(
            String uploadId,
            Path encodedFile,
            String renditionSuffix,
            EncodingExpectations expectations) {
        return ValidationReport.skipped("Remote VVS client not implemented");
    }
}
