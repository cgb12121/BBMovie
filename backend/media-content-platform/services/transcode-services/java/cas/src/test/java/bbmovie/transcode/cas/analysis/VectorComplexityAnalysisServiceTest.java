package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VectorComplexityAnalysisServiceTest {

    @Test
    void shouldProduceStableRiskClassAndHints() {
        DecisionHintsPolicyEngine engine = new DecisionHintsPolicyEngine("v2.0", "policy-v1");
        VectorComplexityAnalysisService service = new VectorComplexityAnalysisService(engine, "v2.0", "policy-v1");
        SourceProfileV2 source = new SourceProfileV2(
                "upload-1",
                "bucket-a",
                "movie.mp4",
                1920,
                1080,
                3600,
                "h264",
                "mp4",
                30.0,
                "cfr",
                2,
                8,
                "yuv420p",
                "fast",
                0.9,
                "v2.0",
                "abc123",
                List.of()
        );

        ComplexityProfileV2 profile = service.analyze(source);

        assertTrue(profile.complexityScore() > 0.0);
        assertTrue(profile.dimensionScores().containsKey("spatialScore"));
        assertTrue(profile.decisionHints().maxRungs() >= 1);
        assertEquals("v2.0", profile.analysisVersion());
        assertEquals("policy-v1", profile.policyVersion());
    }
}
