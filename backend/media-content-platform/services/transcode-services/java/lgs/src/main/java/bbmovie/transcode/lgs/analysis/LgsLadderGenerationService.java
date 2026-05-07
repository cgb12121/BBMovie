package bbmovie.transcode.lgs.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import bbmovie.transcode.contracts.planning.TranscodeLadderTemplates;
import bbmovie.transcode.lgs.dto.LgsRecipeHints;
import bbmovie.transcode.lgs.dto.LgsSourceVideoMetadata;

/**
 * Generates a deterministic LGS rendition ladder from source dimensions and optional recipe hints.
 *
 * <p>Ported from transcode-worker {@code LadderGenerationService} with non-empty ladder safeguards.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class LgsLadderGenerationService {

    private final LgsResolutionCostCalculator costCalculator;

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
        return generateDecisionPlan(metadata, recipeHints).selectedRungs();
    }

    /**
     * Builds a policy decision snapshot for ladder planning (selected vs dropped rungs and reasons).
     */
    public LadderDecisionPlan generateDecisionPlan(LgsSourceVideoMetadata metadata, LgsRecipeHints recipeHints) {
        LgsRecipeHints hints = recipeHints != null ? recipeHints : LgsRecipeHints.none();
        List<LadderRung> ladder = new ArrayList<>();
        Map<String, String> droppedReasons = new HashMap<>();
        for (TranscodeLadderTemplates.LadderPreset def : TranscodeLadderTemplates.baselineHls()) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (ladder.isEmpty()) {
            // Keep one fallback rung when source is below the minimum preset bucket.
            ladder.add(new LadderRung(metadata.width(), metadata.height(), "original"));
        }
        List<LadderRung> beforeSkip = List.copyOf(ladder);
        if (hints.skipSuffixes() != null && !hints.skipSuffixes().isEmpty()) {
            List<LadderRung> preFilter = List.copyOf(ladder);
            ladder = ladder.stream()
                    .filter(res -> !hints.skipSuffixes().contains(res.filename()))
                    .toList();
            for (LadderRung rung : preFilter) {
                if (hints.skipSuffixes().contains(rung.filename())) {
                    droppedReasons.put(rung.filename(), "skip_suffix");
                }
            }
        }
        // Never return an empty ladder; downstream encode planning expects at least one rung.
        if (ladder.isEmpty()) {
            log.warn("skipSuffixes excluded every rung (hints={}); restoring pre-filter ladder", hints.skipSuffixes());
            ladder = new ArrayList<>(beforeSkip);
            droppedReasons.entrySet().removeIf(e -> "skip_suffix".equals(e.getValue()));
        }

        // Netflix-style budget pressure: if bitrate bias is low, prefer removing the highest rung first.
        if (hints.bitrateBias() < 0.90 && ladder.size() > Math.max(1, hints.minRungs())) {
            List<LadderRung> descByHeight = ladder.stream()
                    .sorted(Comparator.comparingInt(LadderRung::height).reversed())
                    .collect(Collectors.toCollection(ArrayList::new));
            while (descByHeight.size() > Math.max(1, hints.minRungs()) && hints.bitrateBias() < 0.90) {
                LadderRung dropped = descByHeight.removeFirst();
                droppedReasons.putIfAbsent(dropped.filename(), "bitrate_pressure");
                // only drop one rung for now to keep behavior predictable.
                break;
            }
            ladder = descByHeight.stream()
                    .sorted(Comparator.comparingInt(LadderRung::height).reversed())
                    .toList();
        }

        int maxRungs = Math.max(1, hints.maxRungs());
        if (ladder.size() > maxRungs) {
            List<LadderRung> kept = ladder.stream().limit(maxRungs).toList();
            for (LadderRung rung : ladder) {
                if (!kept.contains(rung)) {
                    droppedReasons.putIfAbsent(rung.filename(), "max_rungs");
                }
            }
            ladder = kept;
        }

        if (ladder.size() < Math.max(1, hints.minRungs())) {
            List<LadderRung> mutable = new ArrayList<>(ladder);
            List<LadderRung> source = beforeSkip;
            for (LadderRung rung : source) {
                if (mutable.size() >= hints.minRungs()) {
                    break;
                }
                if (!mutable.contains(rung)) {
                    mutable.add(rung);
                    droppedReasons.remove(rung.filename());
                }
            }
            ladder = mutable;
        }

        ladder = ladder.stream()
                .sorted(Comparator.comparingInt(LadderRung::height).reversed())
                .toList();

        log.info("LGS generated ladder for {}x{}: {}",
                metadata.width(), metadata.height(),
                ladder.stream().map(LadderRung::filename).toList());
        return new LadderDecisionPlan(
                "lgs-netflix-v1",
                ladder,
                droppedReasons,
                calculatePeakCost(toSuffixes(ladder)),
                calculateTotalCost(toSuffixes(ladder))
        );
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

    public record LadderDecisionPlan(
            String policyVersion,
            List<LadderRung> selectedRungs,
            Map<String, String> droppedReasons,
            int peakCost,
            int totalCost
    ) {
    }
}
