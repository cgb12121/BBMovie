package bbmovie.community.student_program_service.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
@ConditionalOnProperty(prefix = "student-program.temporal", name = "enabled", havingValue = "true")
public class TemporalConfig {
    private WorkerFactory workerFactory;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
        WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                .setTarget(properties.getTarget())
                .build();
        return WorkflowServiceStubs.newServiceStubs(options);
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs, TemporalProperties properties) {
        WorkflowClientOptions options = WorkflowClientOptions.newBuilder()
                .setNamespace(properties.getNamespace())
                .build();
        return WorkflowClient.newInstance(serviceStubs, options);
    }

    @Bean
    public WorkerFactory temporalWorkerFactory(WorkflowClient workflowClient,
                                               StudentVerificationActivities activities,
                                               TemporalProperties properties) {
        this.workerFactory = WorkerFactory.newInstance(workflowClient);
        Worker worker = workerFactory.newWorker(properties.getTaskQueue());
        worker.registerWorkflowImplementationTypes(StudentVerificationWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        workerFactory.start();
        log.info("Temporal worker started on taskQueue={}", properties.getTaskQueue());
        return workerFactory;
    }

    @PreDestroy
    public void shutdownFactory() {
        if (workerFactory != null) {
            workerFactory.shutdown();
        }
    }
}
