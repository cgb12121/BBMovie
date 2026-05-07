package bbmovie.transcode.cas.dto;

/**
 * Minimal source metadata needed by CAS complexity and ladder policies.
 *
 * <p>Portable shape adapted from transcode-worker FFmpeg metadata model.</p>
 */
public record SourceVideoMetadata(int width, int height, double duration, String codec) {
}
