package bbmovie.transcode.vqs.config;

import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.vqs.activity.VqsMediaActivities;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Registers and starts VQS activities on Temporal quality task queue. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "vqsWorkerFactory")
@ConditionalOnProperty(name = "vqs.worker.register", havingValue = "true", matchIfMissing = false)
public class VqsWorkerLifecycle {

    private final WorkerFactory vqsWorkerFactory;
    private final VqsMediaActivities vqsMediaActivities;

    /** Binds VQS activities to QUALITY queue and starts worker polling. */
    @PostConstruct
    public void startWorker() {
        Worker worker = vqsWorkerFactory.newWorker(TemporalTaskQueues.QUALITY);
        worker.registerActivitiesImplementations(vqsMediaActivities);
        vqsWorkerFactory.start();
        log.info("VQS Video Quality Service: registered {} on {}", vqsMediaActivities.getClass().getSimpleName(),
                TemporalTaskQueues.QUALITY);
    }
}
