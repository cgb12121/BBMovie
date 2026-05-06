package bbmovie.transcode.vis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class VisMediaProcessingProperties {

    private String ffprobePath = "ffprobe";
    private boolean profileV2Enabled = true;
    private String profileV2AnalysisVersion = "v2.0";
    private int profileV2MinDurationSecondsForTrust = 1;
    private int profileV2MinWidthForTrust = 240;
}
