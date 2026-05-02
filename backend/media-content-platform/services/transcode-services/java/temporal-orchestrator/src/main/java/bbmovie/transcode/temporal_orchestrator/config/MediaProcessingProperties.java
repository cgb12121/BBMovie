package bbmovie.transcode.temporal_orchestrator.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class MediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String moviesKeyPrefix = "movies";
    private String ffmpegPath = "ffmpeg";
    private String ffprobePath = "ffprobe";
    private String tempDir = System.getProperty("java.io.tmpdir");
}
