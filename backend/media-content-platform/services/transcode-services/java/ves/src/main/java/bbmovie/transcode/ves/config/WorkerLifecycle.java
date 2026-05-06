package bbmovie.transcode.ves.config;

import bbmovie.transcode.contracts.temporal.TemporalTaskQueues;
import bbmovie.transcode.ves.activity.EncodingActivities;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(name = "workerFactory")
@ConditionalOnProperty(name = "ves.worker.register", havingValue = "true", matchIfMissing = true)
public class WorkerLifecycle {

    private final WorkerFactory workerFactory;
    private final EncodingActivities encodingActivities;
    private final TemporalProperties temporalProperties;

    @PostConstruct
    public void startWorker() {
        WorkerOptions workerOptions = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(temporalProperties.getMaxConcurrentActivityExecutions())
                .setMaxConcurrentActivityTaskPollers(temporalProperties.getMaxConcurrentActivityTaskPollers())
                .build();
        Worker worker = workerFactory.newWorker(TemporalTaskQueues.ENCODING, workerOptions);
        worker.registerActivitiesImplementations(encodingActivities);
        workerFactory.start();
        log.info(
                "VES registered {} on {} with maxConcurrentActivityExecutions={} maxConcurrentActivityTaskPollers={}",
                encodingActivities.getClass().getSimpleName(),
                TemporalTaskQueues.ENCODING,
                temporalProperties.getMaxConcurrentActivityExecutions(),
                temporalProperties.getMaxConcurrentActivityTaskPollers()
        );
    }
}
