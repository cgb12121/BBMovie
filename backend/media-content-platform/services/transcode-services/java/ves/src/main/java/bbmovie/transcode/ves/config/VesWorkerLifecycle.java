package bbmovie.transcode.ves.config;

import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.ves.activity.VesMediaActivities;
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
public class VesWorkerLifecycle {

    private final WorkerFactory workerFactory;
    private final VesMediaActivities vesMediaActivities;

    @PostConstruct
    public void startWorker() {
        Worker worker = workerFactory.newWorker(TemporalTaskQueues.ENCODING);
        worker.registerActivitiesImplementations(vesMediaActivities);
        workerFactory.start();
        log.info("VES Video Encoding Service: registered {} on task queue {}", vesMediaActivities.getClass().getSimpleName(),
                TemporalTaskQueues.ENCODING);
    }
}
