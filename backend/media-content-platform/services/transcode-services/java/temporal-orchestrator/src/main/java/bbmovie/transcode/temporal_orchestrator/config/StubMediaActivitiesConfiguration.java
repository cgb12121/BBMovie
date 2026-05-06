package bbmovie.transcode.temporal_orchestrator.config;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.temporal_orchestrator.stub.StubMediaActivities;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StubMediaActivitiesConfiguration {

    @Bean
    @ConditionalOnMissingBean(MediaActivities.class)
    @ConditionalOnProperty(name = "temporal.activity-implementation", havingValue = "stub", matchIfMissing = true)
    public MediaActivities stubMediaActivities() {
        return new StubMediaActivities();
    }
}
