package bbmovie.transcode.vqs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Temporal connectivity properties for VQS worker runtime. */
@Getter
@Setter
@ConfigurationProperties(prefix = "temporal")
public class VqsTemporalProperties {

    private boolean enabled = true;
    private String target = "localhost:7233";
    private String namespace = "default";
}
