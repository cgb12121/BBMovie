package bbmovie.transcode.vvs.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(VvsTemporalProperties.class)
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class VvsTemporalConfiguration {

    private final VvsTemporalProperties vvsTemporalProperties;

    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs vvsWorkflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(vvsTemporalProperties.getTarget())
                        .build()
        );
    }

    @Bean
    public WorkflowClient vvsWorkflowClient(WorkflowServiceStubs vvsWorkflowServiceStubs) {
        return WorkflowClient.newInstance(
                vvsWorkflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(vvsTemporalProperties.getNamespace())
                        .build()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public WorkerFactory vvsWorkerFactory(WorkflowClient vvsWorkflowClient) {
        return WorkerFactory.newInstance(vvsWorkflowClient);
    }
}
