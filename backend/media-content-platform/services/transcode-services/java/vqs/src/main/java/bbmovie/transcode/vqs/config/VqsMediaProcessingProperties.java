package bbmovie.transcode.vqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
/** Media-processing runtime properties consumed by VQS validation service. */
public class VqsMediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String tempDir = System.getProperty("java.io.tmpdir");
    private String ffprobePath = "ffprobe";
}
