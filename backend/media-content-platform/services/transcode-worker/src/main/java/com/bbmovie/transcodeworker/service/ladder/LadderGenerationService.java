package com.bbmovie.transcodeworker.service.ladder;

import com.bbmovie.transcodeworker.service.complexity.dto.RecipeHints;
import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService.VideoResolution;
import com.bbmovie.transcodeworker.service.scheduler.ResolutionCostCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LGS-lite service for generating bitrate ladders.
 * <p>
 * For now, this uses source-height based presets and shared cost weights.
 * It centralizes ladder decisions so probing and transcoding use one policy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LadderGenerationService {

    private final ResolutionCostCalculator costCalculator;

    /**
     * Preset ladder definitions in descending quality order.
     */
    private static final List<ResolutionDefinition> PRESET_LADDER = List.of(
            new ResolutionDefinition(1080, 1920, 1080, "1080p"),
            new ResolutionDefinition(720, 1280, 720, "720p"),
            new ResolutionDefinition(480, 854, 480, "480p"),
            new ResolutionDefinition(360, 640, 360, "360p"),
            new ResolutionDefinition(240, 426, 240, "240p"),
            new ResolutionDefinition(144, 256, 144, "144p")
    );

    /**
     * Lookup table for turning ladder suffixes into output resolutions.
     */
    private static final Map<String, VideoResolution> RESOLUTION_LOOKUP = createResolutionLookup();

    public List<VideoResolution> generateEncodingLadder(FFmpegVideoMetadata metadata) {
        return generateEncodingLadder(metadata, RecipeHints.none());
    }

    public List<VideoResolution> generateEncodingLadder(FFmpegVideoMetadata metadata, RecipeHints recipeHints) {
        List<VideoResolution> ladder = new ArrayList<>();

        for (ResolutionDefinition def : PRESET_LADDER) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new VideoResolution(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }

        if (ladder.isEmpty()) {
            ladder.add(new VideoResolution(metadata.width(), metadata.height(), "original"));
        }

        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            ladder = ladder.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
        }

        log.info("Generated ladder for source {}x{}: {}",
                metadata.width(),
                metadata.height(),
                ladder.stream().map(VideoResolution::filename).toList());
        return ladder;
    }

    /**
     * Applies CAS recipe hints (e.g. skip rungs for low-complexity content).
     */
    public List<VideoResolution> applyRecipeHints(List<VideoResolution> ladder, RecipeHints hints) {
        if (hints == null || hints.skipSuffixes() == null || hints.skipSuffixes().isEmpty()) {
            return ladder;
        }
        List<VideoResolution> filtered = ladder.stream()
                .filter(r -> !hints.skipSuffixes().contains(r.filename()))
                .toList();
        if (filtered.isEmpty()) {
            log.warn("Recipe hints removed all ladder rungs; keeping original ladder");
            return ladder;
        }
        log.info("Applied CAS recipe hints, remaining rungs: {}",
                filtered.stream().map(VideoResolution::filename).toList());
        return filtered;
    }

    public List<VideoResolution> resolveEncodingLadder(List<String> ladderSuffixes, FFmpegVideoMetadata metadata) {
        return resolveEncodingLadder(ladderSuffixes, metadata, RecipeHints.none());
    }

    public List<VideoResolution> resolveEncodingLadder(
            List<String> ladderSuffixes, FFmpegVideoMetadata metadata, RecipeHints recipeHints) {
        if (ladderSuffixes == null || ladderSuffixes.isEmpty()) {
            return generateEncodingLadder(metadata, recipeHints);
        }

        List<VideoResolution> resolved = new ArrayList<>();
        for (String suffix : ladderSuffixes) {
            VideoResolution preset = RESOLUTION_LOOKUP.get(suffix);
            if (preset != null) {
                resolved.add(preset);
            } else if ("original".equalsIgnoreCase(suffix)) {
                resolved.add(new VideoResolution(metadata.width(), metadata.height(), "original"));
            } else {
                log.warn("Unknown ladder suffix '{}', skipping", suffix);
            }
        }

        if (resolved.isEmpty()) {
            return generateEncodingLadder(metadata, recipeHints);
        }
        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            return resolved.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
        }
        return resolved;
    }

    public List<String> toSuffixes(List<VideoResolution> ladder) {
        return ladder.stream().map(VideoResolution::filename).toList();
    }

    public int calculatePeakCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream()
                .mapToInt(costCalculator::calculateCost)
                .max()
                .orElse(1);
    }

    public int calculateTotalCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream()
                .mapToInt(costCalculator::calculateCost)
                .sum();
    }

    private static Map<String, VideoResolution> createResolutionLookup() {
        Map<String, VideoResolution> lookup = new LinkedHashMap<>();
        for (ResolutionDefinition def : PRESET_LADDER) {
            lookup.put(def.suffix(), new VideoResolution(def.targetWidth(), def.targetHeight(), def.suffix()));
        }
        return lookup;
    }

    private record ResolutionDefinition(int minHeight, int targetWidth, int targetHeight, String suffix) {}
}
