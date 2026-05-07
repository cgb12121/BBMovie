package bbmovie.transcode.vis.probe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import bbmovie.transcode.contracts.planning.TranscodeLadderTemplates;
import bbmovie.transcode.vis.dto.VisSourceVideoMetadata;

/**
 * VIS ladder generator used during probe to estimate downstream encode plan and cost.
 */
@Slf4j
@RequiredArgsConstructor
public class VisLadderGenerationService {

    private final VisResolutionCostCalculator costCalculator;

    /** Generates baseline ladder by source height, with original fallback for tiny sources. */
    public List<LadderRung> generateEncodingLadder(VisSourceVideoMetadata metadata) {
        List<LadderRung> ladder = new ArrayList<>();
        for (TranscodeLadderTemplates.LadderPreset def : TranscodeLadderTemplates.baselineHls()) {
            if (metadata.height() >= def.minHeight()) {
                ladder.add(new LadderRung(def.targetWidth(), def.targetHeight(), def.suffix()));
            }
        }
        if (ladder.isEmpty()) {
            ladder.add(new LadderRung(metadata.width(), metadata.height(), "original"));
        }
        log.info("VIS generated ladder for {}x{}: {}", metadata.width(), metadata.height(), ladder.stream().map(LadderRung::filename).toList());
        return ladder;
    }

    /** Converts ladder entries into suffix labels used in probe outcome payloads. */
    public List<String> toSuffixes(List<LadderRung> ladder) {
        return ladder.stream().map(LadderRung::filename).toList();
    }

    /** Returns peak per-rung cost estimate. */
    public int calculatePeakCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).max().orElse(1);
    }

    /** Returns total summed ladder cost estimate. */
    public int calculateTotalCost(List<String> ladderSuffixes) {
        return ladderSuffixes.stream().mapToInt(costCalculator::calculateCost).sum();
    }

    public record LadderRung(int width, int height, String filename) {
    }
}
