package bbmovie.transcode.lgs.analysis;

/**
 * Ported from transcode-worker {@code FFmpegVideoMetadata} for ladder generation.
 */
public record LgsSourceVideoMetadata(int width, int height, double duration, String codec) {
}
