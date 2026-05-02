package bbmovie.transcode.cas.analysis;

/**
 * Ported from transcode-worker {@code ComplexityAnalysisService}.
 */
public interface ComplexityAnalysisService {

    ComplexityProfile analyze(String uploadId, SourceVideoMetadata metadata);
}
