package com.bbmovie.transcodeworker.port.remote;

import com.bbmovie.transcodeworker.port.VideoQualityPort;
import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "true")
public class RemoteVideoQualityClient implements VideoQualityPort {

    @Override
    public QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix) {
        return new QualityReport(
                renditionSuffix,
                "VMAF",
                0.0,
                null,
                null,
                false,
                null,
                "Remote VQS client not implemented",
                java.time.Instant.now()
        );
    }
}
