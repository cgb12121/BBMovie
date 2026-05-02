package bbmovie.transcode.vis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class VisMediaProcessingProperties {

    private String ffprobePath = "ffprobe";
}
