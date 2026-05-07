package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.cas.dto.RecipeHints;
import bbmovie.transcode.cas.dto.SourceVideoMetadata;
import bbmovie.transcode.contracts.dto.DecisionHintsV2;
import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasLadderGenerationServiceAdaptiveTest {

    @Test
    void shouldApplySkipAndMaxRungsFromHints() {
        CasLadderGenerationService service = new CasLadderGenerationService(new ResolutionCostCalculator());
        SourceVideoMetadata metadata = new SourceVideoMetadata(1920, 1080, 1800, "h264");
        DecisionHintsV2 hints = new DecisionHintsV2(
                "medium",
                "HIGH",
                2,
                1000,
                8000,
                true,
                List.of("1080p"),
                Map.of("motionScore", 0.8),
                List.of("test"),
                "v2.0",
                "policy-v1",
                EncodeBitrateStrategy.VBV_ABR,
                null
        );

        List<CasLadderGenerationService.LadderRung> ladder = service.generateAdaptiveEncodingLadder(
                metadata,
                RecipeHints.skip(Set.of()),
                hints
        );

        assertFalse(ladder.isEmpty());
        assertTrue(ladder.size() <= 2);
        assertTrue(ladder.stream().noneMatch(r -> "1080p".equals(r.filename())));
    }
}
