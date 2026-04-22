package com.bbmovie.transcodeworker.service.complexity;

import com.bbmovie.transcodeworker.service.complexity.dto.ComplexityProfile;
import com.bbmovie.transcodeworker.service.complexity.dto.RecipeHints;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@ConditionalOnProperty(prefix = "app.analysis.cas", name = "enabled", havingValue = "true")
public class HeuristicComplexityAnalysisService implements ComplexityAnalysisService {

    @Override
    public ComplexityProfile analyze(String uploadId, FFmpegVideoMetadata metadata) {
        double resolutionFactor = Math.min(1.0, metadata.height() / 1080.0);
        double durationFactor = Math.min(1.0, metadata.duration() / 7200.0);
        double codecFactor = "hevc".equalsIgnoreCase(metadata.codec()) ? 0.8 : 1.0;
        double complexityScore = Math.max(0.05, (resolutionFactor * 0.6 + durationFactor * 0.4) * codecFactor);

        String contentClass = complexityScore >= 0.8 ? "high"
                : complexityScore >= 0.5 ? "medium"
                : "low";

        Set<String> skip = new HashSet<>();
        if (metadata.height() < 1080) {
            skip.add("1080p");
        }
        if (metadata.height() < 720) {
            skip.add("720p");
        }
        if (metadata.height() < 480) {
            skip.add("480p");
        }
        if (metadata.duration() > 7200) {
            skip.add("144p");
        }

        Map<String, Double> features = new HashMap<>();
        features.put("resolutionFactor", resolutionFactor);
        features.put("durationFactor", durationFactor);
        features.put("codecFactor", codecFactor);

        return new ComplexityProfile(
                uploadId,
                contentClass,
                complexityScore,
                features,
                RecipeHints.skip(skip),
                Instant.now()
        );
    }
}
