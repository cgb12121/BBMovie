package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexityFixtureTest {

    @Test
    void shouldAnalyzeHighMotionFixtureWithNonLowRisk() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/complexity/high_motion_sports.json")) {
            SourceProfileV2 source = mapper.readValue(in, SourceProfileV2.class);
            VectorComplexityAnalysisService service = new VectorComplexityAnalysisService(
                    new DecisionHintsPolicyEngine("v2.0", "policy-v1"),
                    "v2.0",
                    "policy-v1"
            );
            ComplexityProfileV2 profile = service.analyze(source);
            assertTrue(profile.complexityScore() >= 0.35);
            assertTrue(!"LOW".equals(profile.riskClass()));
        }
    }
}
