package bbmovie.transcode.vis.probe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Ported from transcode-worker {@code MetadataService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisMetadataService {

    private final FFprobe ffprobe;

    public VisSourceVideoMetadata getMetadata(Path videoPath) {
        return getMetadataFromSource(videoPath.toString());
    }

    public VisSourceVideoMetadata getMetadataFromUrl(String url) {
        log.debug("Probing video from URL (truncated): {}...", url.substring(0, Math.min(url.length(), 80)));
        return getMetadataFromSource(url);
    }

    public FFmpegProbeResult probeResultFromPath(Path videoPath) {
        return probeResult(videoPath.toString());
    }

    public FFmpegProbeResult probeResultFromUrl(String url) {
        log.debug("Probing full metadata from URL (truncated): {}...", url.substring(0, Math.min(url.length(), 80)));
        return probeResult(url);
    }

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

    private FFmpegProbeResult probeResult(String source) {
        try {
            return ffprobe.probe(source);
        } catch (IOException e) {
            log.error("Failed to get video metadata from source: {}", source, e);
            throw new RuntimeException("Failed to get video metadata", e);
        }
    }
}
