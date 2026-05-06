package bbmovie.transcode.cas.analysis;

/**
 * Ported from transcode-worker {@code ComplexityAnalysisService}.
 */
public interface ComplexityAnalysisService {

    /** Analyze source metadata using legacy heuristic model. */
    ComplexityProfile analyze(String uploadId, SourceVideoMetadata metadata);
}
