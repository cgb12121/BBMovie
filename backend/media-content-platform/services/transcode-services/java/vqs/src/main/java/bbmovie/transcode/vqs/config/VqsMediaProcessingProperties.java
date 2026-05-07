package bbmovie.transcode.vqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Media-processing runtime properties consumed by VQS validation service. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class VqsMediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String tempDir = System.getProperty("java.io.tmpdir");
    private String ffprobePath = "ffprobe";
    private String ffmpegPath = "ffmpeg";
    private boolean vmafEnabled = true;
    private double vmafPassThresholdMean = 93.0;
    private double vmafPassThresholdP10 = 88.0;
    private double vmafPassThresholdWorstWindow = 85.0;
    private int vmafWorstWindowSeconds = 3;
    private int vmafThreads = 4;
    private int vmafTimeoutSeconds = 1800;
    private String vmafModelPath = "";
}
