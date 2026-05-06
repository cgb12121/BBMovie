package bbmovie.transcode.cas.config;

import bbmovie.transcode.cas.activity.CasMediaActivities;
import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
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
/** Boots CAS activity worker and binds it to the analysis task queue. */
public class CasWorkerLifecycle {

    private final WorkerFactory workerFactory;
    private final CasMediaActivities casMediaActivities;

    @PostConstruct
    /** Registers CAS activities and starts polling on Temporal analysis queue. */
    public void startWorker() {
        Worker worker = workerFactory.newWorker(TemporalTaskQueues.ANALYSIS);
        worker.registerActivitiesImplementations(casMediaActivities);
        workerFactory.start();
        log.info("CAS Complex Analysis Service: registered {} on task queue {}", casMediaActivities.getClass().getSimpleName(),
                TemporalTaskQueues.ANALYSIS);
    }
}
