package bbmovie.transcode.vis.probe;

import bbmovie.transcode.lgs.analysis.LgsSourceVideoMetadata;
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

    public LgsSourceVideoMetadata getMetadata(Path videoPath) {
        return getMetadataFromSource(videoPath.toString());
    }

    public LgsSourceVideoMetadata getMetadataFromUrl(String url) {
        log.debug("Probing video from URL (truncated): {}...", url.substring(0, Math.min(url.length(), 80)));
        return getMetadataFromSource(url);
    }

    private LgsSourceVideoMetadata getMetadataFromSource(String source) {
        try {
            FFmpegProbeResult probeResult = ffprobe.probe(source);
            FFmpegStream videoStream = probeResult.getStreams()
                    .stream()
                    .filter(s -> "video".equalsIgnoreCase(String.valueOf(s.codec_type)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No video stream found"));
            double duration = probeResult.getFormat() != null ? probeResult.getFormat().duration : 0.0;
            return new LgsSourceVideoMetadata(
                    videoStream.width,
                    videoStream.height,
                    duration,
                    videoStream.codec_name
            );
        } catch (IOException e) {
            log.error("Failed to get video metadata from source: {}", source, e);
            throw new RuntimeException("Failed to get video metadata", e);
        }
    }
}
