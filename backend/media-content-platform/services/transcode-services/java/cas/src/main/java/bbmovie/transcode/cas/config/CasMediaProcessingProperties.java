package bbmovie.transcode.cas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
/** CAS media-processing runtime properties bound from application configuration. */
public class CasMediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String moviesKeyPrefix = "movies";
    private String ffprobePath = "ffprobe";
    private String tempDir = System.getProperty("java.io.tmpdir");
    private boolean profileV2Enabled = true;
    private String profileV2AnalysisVersion = "v2.0";
    private String profileV2PolicyVersion = "policy-v1";
}
