package bbmovie.transcode.temporal_orchestrator.config;

import bbmovie.transcode.contracts.activity.MediaActivities;
import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.temporal_orchestrator.workflow.VideoProcessingWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "temporal.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class TemporalWorkerLifecycle {

    private final WorkerFactory workerFactory;
    private final TemporalProperties temporalProperties;
    private final MediaActivities mediaActivities;

    @PostConstruct
    public void startWorkers() {
        Worker orchestratorWorker = workerFactory.newWorker(temporalProperties.getOrchestratorTaskQueue());
        orchestratorWorker.registerWorkflowImplementationTypes(VideoProcessingWorkflowImpl.class);
        log.info("Registered workflow implementation on queue {}", temporalProperties.getOrchestratorTaskQueue());

        if (temporalProperties.isRegisterStubActivityWorkers()) {
            registerActivityWorker(TemporalTaskQueues.ANALYSIS, mediaActivities);
            registerActivityWorker(TemporalTaskQueues.ENCODING, mediaActivities);
            registerActivityWorker(TemporalTaskQueues.QUALITY, mediaActivities);
            registerActivityWorker(TemporalTaskQueues.SUBTITLE, mediaActivities);
            log.info("MediaActivities bean registered on analysis, encoding, quality, subtitle queues (implementation={})",
                    mediaActivities.getClass().getSimpleName());
        }

        workerFactory.start();
        log.info("Temporal WorkerFactory started");
    }

    private void registerActivityWorker(String taskQueue, MediaActivities implementation) {
        Worker w = workerFactory.newWorker(taskQueue);
        w.registerActivitiesImplementations(implementation);
        log.info("Registered activity implementation on queue {}", taskQueue);
    }
}
