package bbmovie.transcode.vvs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Media-processing runtime properties consumed by VVS validation service. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class VvsMediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String tempDir = System.getProperty("java.io.tmpdir");
    private String ffprobePath = "ffprobe";

    /** Enables segment-level probing for playback-safety signals (slower than playlist-only checks). */
    private boolean vvsDeepValidationEnabled = true;

    /** Number of segments to download/probe (typically 1–2) when deep validation is enabled. */
    private int vvsDeepSampleSegments = 2;

    /** Max seconds to allow for one segment download before failing deep validation. */
    private int segmentDownloadTimeoutSeconds = 30;

    /** Comma-separated allowlist of video codecs (ffprobe {@code codec_name}). Empty means allow all. */
    private String allowedVideoCodecs = "h264,hevc,av1";

    /** Comma-separated allowlist of audio codecs (ffprobe {@code codec_name}). Empty means allow all. */
    private String allowedAudioCodecs = "aac,eac3,opus";

    /** Comma-separated allowlist of pixel formats (ffprobe {@code pix_fmt}). Empty means allow all. */
    private String allowedPixFmts = "yuv420p,yuv420p10le";

    /** Comma-separated allowlist of audio channel counts. Empty means allow all. */
    private String allowedAudioChannels = "2,6";

    /** Comma-separated allowlist of audio sample rates. Empty means allow all. */
    private String allowedAudioSampleRates = "48000";

    /** Whether at least one audio stream is required for a rendition to be considered valid. */
    private boolean requireAudioStream = true;
}
