package bbmovie.transcode.cas.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Temporal connectivity properties used by CAS worker bootstrap. */
@Getter
@Setter
@ConfigurationProperties(prefix = "temporal")
public class CasTemporalProperties {

    private boolean enabled = true;
    private String target = "localhost:7233";
    private String namespace = "default";
}
