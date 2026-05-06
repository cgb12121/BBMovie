package bbmovie.transcode.cas.config;

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
@EnableConfigurationProperties(CasTemporalProperties.class)
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
/** Temporal client/worker factory wiring for CAS worker process. */
public class CasTemporalConfiguration {

    private final CasTemporalProperties casTemporalProperties;

    @Bean(destroyMethod = "shutdown")
    /** gRPC stubs used by Temporal SDK to communicate with cluster. */
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(casTemporalProperties.getTarget())
                        .build()
        );
    }

    @Bean
    /** CAS-scoped Temporal client bound to configured namespace. */
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(
                workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(casTemporalProperties.getNamespace())
                        .build()
        );
    }

    @Bean(destroyMethod = "shutdown")
    /** Worker factory that creates and runs CAS activity workers. */
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }
}
