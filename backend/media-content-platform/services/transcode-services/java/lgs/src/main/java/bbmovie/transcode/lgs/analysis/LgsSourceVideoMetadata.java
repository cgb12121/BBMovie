package bbmovie.transcode.lgs.analysis;

/**
 * Minimal source metadata required by LGS ladder generation.
 *
 * <p>Adapted from transcode-worker {@code FFmpegVideoMetadata}.</p>
 */
public record LgsSourceVideoMetadata(int width, int height, double duration, String codec) {
}
