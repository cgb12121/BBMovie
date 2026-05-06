package bbmovie.transcode.ves.config;

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

/** Temporal client/worker-factory wiring for VES encoding workers. */
@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TemporalConfiguration {

    private final TemporalProperties temporalProperties;

    @Bean(destroyMethod = "shutdown")
    /** Temporal service stubs used for RPC communication with cluster target. */
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalProperties.getTarget())
                        .build()
        );
    }

    @Bean
    /** Namespace-scoped Temporal workflow client for VES process. */
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(temporalProperties.getNamespace())
                        .build()
        );
    }

    @Bean(destroyMethod = "shutdown")
    /** Worker factory used to register encoding activities. */
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }
}
