package bbmovie.transcode.vvs.config;

import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.vvs.activity.VvsMediaActivities;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Registers and starts VVS activities on Temporal quality task queue. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "vvsWorkerFactory")
@ConditionalOnProperty(name = "vvs.worker.register", havingValue = "true", matchIfMissing = true)
public class VvsWorkerLifecycle {

    private final WorkerFactory vvsWorkerFactory;
    private final VvsMediaActivities vvsMediaActivities;

    @PostConstruct
    /** Binds VVS activities to QUALITY queue and starts worker polling. */
    public void startWorker() {
        Worker worker = vvsWorkerFactory.newWorker(TemporalTaskQueues.QUALITY);
        worker.registerActivitiesImplementations(vvsMediaActivities);
        vvsWorkerFactory.start();
        log.info("VVS Video Validation Service: registered {} on {}", vvsMediaActivities.getClass().getSimpleName(),
                TemporalTaskQueues.QUALITY);
    }
}
