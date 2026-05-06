package bbmovie.transcode.ves.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "temporal")
public class TemporalProperties {

    private boolean enabled = true;
    private String target = "localhost:7233";
    private String namespace = "default";
    /**
     * Number of encode activities this worker may execute concurrently.
     */
    private int maxConcurrentActivityExecutions = 4;
    /**
     * Number of parallel pollers pulling tasks from encoding-queue.
     */
    private int maxConcurrentActivityTaskPollers = 2;
}
