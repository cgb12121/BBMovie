package bbmovie.transcode.lgs.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a deterministic LGS rendition ladder from source dimensions and optional recipe hints.
 *
 * <p>Ported from transcode-worker {@code LadderGenerationService} with non-empty ladder safeguards.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LgsLadderGenerationService {

    private final LgsResolutionCostCalculator costCalculator;

    // Deterministic baseline ladder; higher rungs are included only when source height permits.
    private static final List<ResolutionDefinition> PRESET_LADDER = List.of(
            new ResolutionDefinition(1080, 1920, 1080, "1080p"),
            new ResolutionDefinition(720, 1280, 720, "720p"),
            new ResolutionDefinition(480, 854, 480, "480p"),
            new ResolutionDefinition(360, 640, 360, "360p"),
            new ResolutionDefinition(240, 426, 240, "240p"),
            new ResolutionDefinition(144, 256, 144, "144p")
    );

    /** Builds baseline ladder with no hint overrides. */
    public List<LadderRung> generateEncodingLadder(LgsSourceVideoMetadata metadata) {
        return generateEncodingLadder(metadata, LgsRecipeHints.none());
    }

    /**
     * Builds ladder from source height and applies skipSuffixes when provided.
     *
     * <p>If all rungs are filtered out, original pre-filter ladder is restored.</p>
     */
    public List<LadderRung> generateEncodingLadder(LgsSourceVideoMetadata metadata, LgsRecipeHints recipeHints) {
        List<LadderRung> ladder = new ArrayList<>();
        for (ResolutionDefinition def : PRESET_LADDER) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (ladder.isEmpty()) {
            // Keep one fallback rung when source is below the minimum preset bucket.
            ladder.add(new LadderRung(metadata.width(), metadata.height(), "original"));
        }
        List<LadderRung> beforeSkip = List.copyOf(ladder);
        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            ladder = ladder.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
        }
        // Never return an empty ladder; downstream encode planning expects at least one rung.
        if (ladder.isEmpty()) {
            log.warn("skipSuffixes excluded every rung (hints={}); restoring pre-filter ladder", recipeHints != null ? recipeHints.skipSuffixes() : null);
            ladder = new ArrayList<>(beforeSkip);
        }
        log.info("LGS generated ladder for {}x{}: {}",
                metadata.width(), metadata.height(),
                ladder.stream().map(LadderRung::filename).toList());
        return ladder;
    }

    /** Converts generated ladder rungs into suffix labels for orchestration payloads. */
    public List<String> toSuffixes(List<LadderRung> ladder) {
        return ladder.stream().map(LadderRung::filename).toList();
    }

    /** Returns max per-rung cost weight used for peak capacity estimation. */
    public int calculatePeakCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).max().orElse(1);
    }

    /** Returns sum cost across all selected rungs. */
    public int calculateTotalCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).sum();
    }

    public record LadderRung(int width, int height, String filename) {
    }

    private record ResolutionDefinition(int minHeight, int targetWidth, int targetHeight, String suffix) {
    }
}
