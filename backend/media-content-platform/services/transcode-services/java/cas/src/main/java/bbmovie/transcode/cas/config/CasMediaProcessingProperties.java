package bbmovie.transcode.cas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.media-processing")
public class CasMediaProcessingProperties {

    private String hlsBucket = "bbmovie-hls";
    private String moviesKeyPrefix = "movies";
    private String ffprobePath = "ffprobe";
    private String tempDir = System.getProperty("java.io.tmpdir");
}
