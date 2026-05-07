package bbmovie.transcode.vis.probe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Component;

import bbmovie.transcode.vis.dto.VisSourceVideoMetadata;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Thin ffprobe wrapper used by VIS probe strategies.
 *
 * <p>Provides both summarized metadata and full probe result accessors for fast/deep flows.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisMetadataService {

    private final FFprobe ffprobe;

    /** Extracts minimal source metadata from local file path. */
    public VisSourceVideoMetadata getMetadata(Path videoPath) {
        return getMetadataFromSource(videoPath.toString());
    }

    /** Extracts minimal source metadata from URL source (e.g., presigned MinIO URL). */
    public VisSourceVideoMetadata getMetadataFromUrl(String url) {
        log.debug("Probing video from URL (truncated): {}...", url.substring(0, Math.min(url.length(), 80)));
        return getMetadataFromSource(url);
    }

    /** Returns full ffprobe result from local file path. */
    public FFmpegProbeResult probeResultFromPath(Path videoPath) {
        return probeResult(videoPath.toString());
    }

    /** Returns full ffprobe result from URL source. */
    public FFmpegProbeResult probeResultFromUrl(String url) {
        log.debug("Probing full metadata from URL (truncated): {}...", url.substring(0, Math.min(url.length(), 80)));
        return probeResult(url);
    }

    /** Builds compact metadata object from full ffprobe output. */
    private VisSourceVideoMetadata getMetadataFromSource(String source) {
        FFmpegProbeResult probeResult = probeResult(source);
        FFmpegStream videoStream = probeResult.getStreams()
                .stream()
                .filter(s -> "video".equalsIgnoreCase(String.valueOf(s.codec_type)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No video stream found"));
        double duration = probeResult.getFormat() != null ? probeResult.getFormat().duration : 0.0;
        return new VisSourceVideoMetadata(
                videoStream.width,
                videoStream.height,
                duration,
                videoStream.codec_name
        );
    }

    /** Executes ffprobe and converts checked IO errors into runtime failures. */
    private FFmpegProbeResult probeResult(String source) {
        try {
            return ffprobe.probe(source);
        } catch (IOException e) {
            log.error("Failed to get video metadata from source: {}", source, e);
            throw new RuntimeException("Failed to get video metadata", e);
        }
    }
}
