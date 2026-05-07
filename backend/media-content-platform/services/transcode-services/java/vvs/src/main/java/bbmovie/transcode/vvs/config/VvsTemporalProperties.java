package bbmovie.transcode.vvs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Temporal connectivity properties for VVS worker runtime. */
@Getter
@Setter
@ConfigurationProperties(prefix = "temporal")
public class VvsTemporalProperties {

    private boolean enabled = true;
    private String target = "localhost:7233";
    private String namespace = "default";
}
