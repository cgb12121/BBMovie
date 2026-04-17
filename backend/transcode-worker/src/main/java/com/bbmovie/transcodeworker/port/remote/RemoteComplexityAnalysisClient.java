package com.bbmovie.transcodeworker.port.remote;

import com.bbmovie.transcodeworker.port.ComplexityAnalysisPort;
import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "true")
public class RemoteComplexityAnalysisClient implements ComplexityAnalysisPort {

    @Override
    public ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata) {
        return ComplexityProfile.basic(uploadId);
    }
}
