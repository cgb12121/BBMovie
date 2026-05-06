package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.DecisionHintsV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from transcode-worker {@code LadderGenerationService} (preset ladder + recipe hints + cost helpers).
 */
@Slf4j
@RequiredArgsConstructor
public class CasLadderGenerationService {

    private final ResolutionCostCalculator costCalculator;

    // Baseline ABR ladder, ordered from highest to lowest rendition.
    private static final List<ResolutionDefinition> PRESET_LADDER = List.of(
            new ResolutionDefinition(1080, 1920, 1080, "1080p"),
            new ResolutionDefinition(720, 1280, 720, "720p"),
            new ResolutionDefinition(480, 854, 480, "480p"),
            new ResolutionDefinition(360, 640, 360, "360p"),
            new ResolutionDefinition(240, 426, 240, "240p"),
            new ResolutionDefinition(144, 256, 144, "144p")
    );

    private static final Map<String, LadderRung> RESOLUTION_LOOKUP = createResolutionLookup();

    /** Generates the default ladder with no explicit recipe hint overrides. */
    public List<LadderRung> generateEncodingLadder(SourceVideoMetadata metadata) {
        return generateEncodingLadder(metadata, RecipeHints.none());
    }

    /**
     * Builds a baseline ladder from source height, then applies recipe skip rules.
     *
     * <p>If skips remove every rung, the original ladder is kept to avoid an unencodable request.</p>
     */
    public List<LadderRung> generateEncodingLadder(SourceVideoMetadata metadata, RecipeHints recipeHints) {
        List<LadderRung> ladder = new ArrayList<>();

        for (ResolutionDefinition def : PRESET_LADDER) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }

        if (ladder.isEmpty()) {
            ladder.add(new LadderRung(metadata.width(), metadata.height(), "original"));
        }

        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            List<LadderRung> filtered = ladder.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
            if (filtered.isEmpty()) {
                log.warn("Recipe hints removed all ladder rungs; keeping original ladder");
            } else {
                ladder = filtered;
            }
        }

        log.info("Generated ladder for source {}x{}: {}",
                metadata.width(),
                metadata.height(),
                ladder.stream().map(LadderRung::filename).toList());
        return ladder;
    }

    /**
     * Applies policy-time constraints (skip rungs and max count) over the baseline ladder.
     *
     * <p>Recipe hints are applied first, then decision hints trim the remaining candidates.</p>
     */
    public List<LadderRung> generateAdaptiveEncodingLadder(
            SourceVideoMetadata metadata,
            RecipeHints recipeHints,
            DecisionHintsV2 decisionHints) {
        List<LadderRung> ladder = generateEncodingLadder(metadata, recipeHints);
        if (decisionHints == null) {
            return ladder;
        }
        List<LadderRung> filtered = ladder.stream()
                .filter(r -> decisionHints.skipRungs() == null || !decisionHints.skipRungs().contains(r.filename()))
                .toList();
        if (filtered.isEmpty()) {
            filtered = ladder;
        }
        int maxRungs = Math.max(1, decisionHints.maxRungs());
        if (filtered.size() > maxRungs) {
            filtered = filtered.subList(0, maxRungs);
        }
        log.info("Adaptive ladder with policyVersion={} riskClass={} -> {}",
                decisionHints.policyVersion(),
                decisionHints.complexityRiskClass(),
                filtered.stream().map(LadderRung::filename).toList());
        return filtered;
    }

    /** Applies legacy recipe skips to an existing ladder while preserving non-empty output. */
    public List<LadderRung> applyRecipeHints(List<LadderRung> ladder, RecipeHints hints) {
        if (hints == null || hints.skipSuffixes() == null || hints.skipSuffixes().isEmpty()) {
            return ladder;
        }
        List<LadderRung> filtered = ladder.stream()
                .filter(r -> !hints.skipSuffixes().contains(r.filename()))
                .toList();
        if (filtered.isEmpty()) {
            log.warn("Recipe hints removed all ladder rungs; keeping original ladder");
            return ladder;
        }
        log.info("Applied CAS recipe hints, remaining rungs: {}",
                filtered.stream().map(LadderRung::filename).toList());
        return filtered;
    }

    /** Resolves explicit suffixes to ladder rungs; falls back to generated ladder when empty. */
    public List<LadderRung> resolveEncodingLadder(List<String> ladderSuffixes, SourceVideoMetadata metadata) {
        return resolveEncodingLadder(ladderSuffixes, metadata, RecipeHints.none());
    }

    /**
     * Resolves a requested ladder suffix list and re-applies recipe skips.
     *
     * <p>Unknown suffixes are ignored, and all-empty results fall back to generated defaults.</p>
     */
    public List<LadderRung> resolveEncodingLadder(
            List<String> ladderSuffixes, SourceVideoMetadata metadata, RecipeHints recipeHints) {
        if (ladderSuffixes == null || ladderSuffixes.isEmpty()) {
            return generateEncodingLadder(metadata, recipeHints);
        }

        List<LadderRung> resolved = new ArrayList<>();
        for (String suffix : ladderSuffixes) {
            LadderRung preset = RESOLUTION_LOOKUP.get(suffix);
            if (preset != null) {
                resolved.add(preset);
            } else if ("original".equalsIgnoreCase(suffix)) {
                resolved.add(new LadderRung(metadata.width(), metadata.height(), "original"));
            } else {
                log.warn("Unknown ladder suffix '{}', skipping", suffix);
            }
        }

        if (resolved.isEmpty()) {
            return generateEncodingLadder(metadata, recipeHints);
        }
        if (recipeHints != null && recipeHints.skipSuffixes() != null && !recipeHints.skipSuffixes().isEmpty()) {
            List<LadderRung> filtered = resolved.stream()
                    .filter(res -> !recipeHints.skipSuffixes().contains(res.filename()))
                    .toList();
            if (filtered.isEmpty()) {
                log.warn("Recipe hints removed all ladder rungs; keeping original ladder");
                return resolved;
            }
            return filtered;
        }
        return resolved;
    }

    /** Converts ladder rungs into suffix labels used by downstream encode requests. */
    public List<String> toSuffixes(List<LadderRung> ladder) {
        return ladder.stream().map(LadderRung::filename).toList();
    }

    /** Returns the highest per-rung cost as queue capacity planning input. */
    public int calculatePeakCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream()
                .mapToInt(costCalculator::calculateCost)
                .max()
                .orElse(1);
    }

    /** Returns the sum cost across all rungs for total workload estimation. */
    public int calculateTotalCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream()
                .mapToInt(costCalculator::calculateCost)
                .sum();
    }

    private static Map<String, LadderRung> createResolutionLookup() {
        Map<String, LadderRung> lookup = new LinkedHashMap<>();
        for (ResolutionDefinition def : PRESET_LADDER) {
            lookup.put(def.suffix(), new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
        }
        return lookup;
    }

    public record LadderRung(int width, int height, String filename) {
    }

    private record ResolutionDefinition(int minHeight, int targetWidth, int targetHeight, String suffix) {
    }
}
