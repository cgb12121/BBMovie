package com.bbmovie.transcodeworker.port.inprocess;

import com.bbmovie.transcodeworker.port.ComplexityAnalysisPort;
import com.bbmovie.transcodeworker.service.complexity.ComplexityAnalysisService;
import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InProcessComplexityAnalysisAdapter implements ComplexityAnalysisPort {

    private final ComplexityAnalysisService complexityAnalysisService;

    @Override
    public ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata) {
        return complexityAnalysisService.analyze(uploadId, metadata);
    }
}
