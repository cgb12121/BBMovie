package bbmovie.transcode.cas.analysis;

/**
 * Video metadata used by CAS / ladder (ported from {@code com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata}).
 */
public record SourceVideoMetadata(int width, int height, double duration, String codec) {
}
