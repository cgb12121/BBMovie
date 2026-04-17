package com.bbmovie.transcodeworker.port.inprocess;

import com.bbmovie.transcodeworker.port.VideoQualityPort;
import com.bbmovie.transcodeworker.service.quality.VideoQualityService;
import com.bbmovie.transcodeworker.service.quality.dto.QualityReport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.analysis.remote", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InProcessVideoQualityAdapter implements VideoQualityPort {

    private final VideoQualityService videoQualityService;

    @Override
    public QualityReport score(String uploadId, Path sourceFile, Path encodedFile, String renditionSuffix) {
        return videoQualityService.score(uploadId, sourceFile, encodedFile, renditionSuffix);
    }
}
