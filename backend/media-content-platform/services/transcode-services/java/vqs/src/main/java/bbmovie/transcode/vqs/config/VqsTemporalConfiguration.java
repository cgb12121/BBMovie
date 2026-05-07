package bbmovie.transcode.vqs.config;

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

/** Temporal client/worker-factory wiring used by VQS worker process. */
@Configuration
@EnableConfigurationProperties(VqsTemporalProperties.class)
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class VqsTemporalConfiguration {

    private final VqsTemporalProperties vqsTemporalProperties;

    /** Temporal service stubs for RPC calls to configured cluster target. */
    @Bean(destroyMethod = "shutdown")
    public WorkflowServiceStubs vqsWorkflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(vqsTemporalProperties.getTarget())
                        .build()
        );
    }

    /** Namespace-scoped workflow client for VQS worker components. */
    @Bean
    public WorkflowClient vqsWorkflowClient(WorkflowServiceStubs vqsWorkflowServiceStubs) {
        return WorkflowClient.newInstance(
                vqsWorkflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(vqsTemporalProperties.getNamespace())
                        .build()
        );
    }

    /** Worker factory used to register and run VQS activities. */
    @Bean(destroyMethod = "shutdown")
    public WorkerFactory vqsWorkerFactory(WorkflowClient vqsWorkflowClient) {
        return WorkerFactory.newInstance(vqsWorkflowClient);
    }
}
