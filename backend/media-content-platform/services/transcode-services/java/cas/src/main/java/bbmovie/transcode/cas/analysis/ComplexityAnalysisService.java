package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.cas.dto.ComplexityProfile;
import bbmovie.transcode.cas.dto.SourceVideoMetadata;

/**
 * Ported from transcode-worker {@code ComplexityAnalysisService}.
 */
public interface ComplexityAnalysisService {

    /** Analyze source metadata using legacy heuristic model. */
    ComplexityProfile analyze(String uploadId, SourceVideoMetadata metadata);
}
