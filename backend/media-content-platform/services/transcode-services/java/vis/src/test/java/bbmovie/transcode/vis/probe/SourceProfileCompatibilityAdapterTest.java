package bbmovie.transcode.vis.probe;

import bbmovie.transcode.contracts.dto.MetadataDTO;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import bbmovie.transcode.contracts.dto.VisDecisionReportDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourceProfileCompatibilityAdapterTest {

    @Test
    void mapsAnalysisResultWithDecisionReport() {
        SourceProfileCompatibilityAdapter adapter = new SourceProfileCompatibilityAdapter();
        SourceProfileV2 profile = new SourceProfileV2(
                "u1", "raw", "movies/u1.mp4", 1920, 1080, 120.0, "h264", "mp4",
                24.0, "cfr", 2, 8, "yuv420p", "deep", 0.97, "v2.0", "abc123",
                List.of("duration_too_small")
        );
        VisDecisionReportDTO report = new VisDecisionReportDTO(
                "deep", 0.97, List.of("bitrate_outlier"), true, "vis-inspection-v1",
                List.of("duration_too_small"), List.of("1080p", "720p"), 32, 56,
                List.of("probe_mode=fast", "probe_mode=deep")
        );
        VisProfileV2Service.AnalysisResult analysis = new VisProfileV2Service.AnalysisResult(profile, report);

        MetadataDTO metadata = adapter.toMetadataDTO(analysis);

        assertEquals(1920, metadata.width());
        assertEquals("h264", metadata.codec());
        assertNotNull(metadata.visDecisionReport());
        assertEquals("deep", metadata.visDecisionReport().probeMode());
    }

    @Test
    void keepsBackwardCompatibilityWhenNoReportProvided() {
        SourceProfileCompatibilityAdapter adapter = new SourceProfileCompatibilityAdapter();
        SourceProfileV2 profile = new SourceProfileV2(
                "u2", "raw", "movies/u2.mp4", 1280, 720, 60.0, "h264", "mp4",
                24.0, "cfr", 2, 8, "yuv420p", "fast", 0.90, "v2.0", "def456",
                List.of()
        );

        MetadataDTO metadata = adapter.toMetadataDTO(profile);

        assertEquals(1280, metadata.width());
        assertNull(metadata.visDecisionReport());
    }
}

