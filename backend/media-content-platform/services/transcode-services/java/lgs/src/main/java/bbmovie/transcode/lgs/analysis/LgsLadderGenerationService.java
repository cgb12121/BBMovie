package bbmovie.transcode.lgs.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from transcode-worker {@code LadderGenerationService}.
 */
@Slf4j
@RequiredArgsConstructor
public class LgsLadderGenerationService {

    private final LgsResolutionCostCalculator costCalculator;

    private static final List<ResolutionDefinition> PRESET_LADDER = List.of(
            new ResolutionDefinition(1080, 1920, 1080, "1080p"),
            new ResolutionDefinition(720, 1280, 720, "720p"),
            new ResolutionDefinition(480, 854, 480, "480p"),
            new ResolutionDefinition(360, 640, 360, "360p"),
            new ResolutionDefinition(240, 426, 240, "240p"),
            new ResolutionDefinition(144, 256, 144, "144p")
    );

    public List<LadderRung> generateEncodingLadder(LgsSourceVideoMetadata metadata) {
        return generateEncodingLadder(metadata, LgsRecipeHints.none());
    }

    public List<LadderRung> generateEncodingLadder(LgsSourceVideoMetadata metadata, LgsRecipeHints recipeHints) {
        List<LadderRung> ladder = new ArrayList<>();
        for (ResolutionDefinition def : PRESET_LADDER) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (ladder.isEmpty()) {
            ladder.add(new LadderRung(metadata.width(), metadata.height(), "original"));
        }
        List<LadderRung> beforeSkip = List.copyOf(ladder);
        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            ladder = ladder.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
        }
        if (ladder.isEmpty()) {
            log.warn("skipSuffixes excluded every rung (hints={}); restoring pre-filter ladder", recipeHints.skipSuffixes());
            ladder = new ArrayList<>(beforeSkip);
        }
        log.info("LGS generated ladder for {}x{}: {}",
                metadata.width(), metadata.height(),
                ladder.stream().map(LadderRung::filename).toList());
        return ladder;
    }

    public List<String> toSuffixes(List<LadderRung> ladder) {
        return ladder.stream().map(LadderRung::filename).toList();
    }

    public int calculatePeakCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).max().orElse(1);
    }

    public int calculateTotalCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).sum();
    }

    public record LadderRung(int width, int height, String filename) {
    }

    private record ResolutionDefinition(int minHeight, int targetWidth, int targetHeight, String suffix) {
    }
}
