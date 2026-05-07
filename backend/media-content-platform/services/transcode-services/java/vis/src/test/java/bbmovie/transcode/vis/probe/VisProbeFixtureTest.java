package bbmovie.transcode.vis.probe;

import com.fasterxml.jackson.databind.ObjectMapper;

import bbmovie.transcode.vis.dto.VisProbeOutcome;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VisProbeFixtureTest {

    @Test
    void shouldFlagLowConfidenceFixtureForDeepProbe() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/vis/low_confidence_probe.json")) {
            VisProbeOutcome outcome = mapper.readValue(in, VisProbeOutcome.class);
            VisProbeDecisionPolicy policy = new VisProbeDecisionPolicy(1, 240);
            VisProbeDecisionPolicy.ProbeDecision decision = policy.decide(outcome, "sample.dat");
            assertTrue(decision.deepProbeRequired());
            assertTrue(decision.confidence() < 0.8);
        }
    }
}
