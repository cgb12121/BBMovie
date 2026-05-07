package bbmovie.transcode.temporal_orchestrator.config;

import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "temporal")
public class TemporalProperties {

    private String target = "localhost:7233";
    private String namespace = "default";
    private String orchestratorTaskQueue = TemporalTaskQueues.ORCHESTRATOR;
}
