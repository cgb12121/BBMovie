package bbmovie.transcode.temporal_orchestrator.nats;

import bbmovie.transcode.temporal_orchestrator.config.TemporalProperties;
import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import bbmovie.transcode.temporal_orchestrator.workflow.VideoProcessingWorkflow;
import io.nats.client.Connection;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@ConditionalOnBean(Connection.class)
@RequiredArgsConstructor
public class TranscodeWorkflowNatsBridge {

    private final NatsJetStreamBootstrap jetStreamBootstrap;
    private final MinioEventParser minioEventParser;
    private final WorkflowClient workflowClient;
    private final TemporalProperties temporalProperties;
    private final NatsMessageHeartbeat natsMessageHeartbeat;

    @Value("${nats.consumer.fetch-batch-size:5}")
    private int fetchBatchSize;

    @Value("${nats.consumer.fetch-timeout-seconds:2}")
    private int fetchTimeoutSeconds;

    @Value("${app.transcode.nats-bridge.max-ack-pending:4}")
    private int maxAckPending;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private JetStreamSubscription subscription;
    private Thread fetchThread;

    @PostConstruct
    public void start() throws Exception {
        jetStreamBootstrap.initialize();
        jetStreamBootstrap.ensureStreamExists();
        jetStreamBootstrap.setupConsumer(maxAckPending);

        PullSubscribeOptions options = PullSubscribeOptions.builder()
                .durable(jetStreamBootstrap.getConsumerDurable())
                .stream(jetStreamBootstrap.getStreamName())
                .build();

        subscription = jetStreamBootstrap.getJetStream()
                .subscribe(jetStreamBootstrap.getSubject(), options);

        running.set(true);
        fetchThread = Thread.ofVirtual().start(this::fetchLoop);
        log.info("TranscodeWorkflowNatsBridge started");
    }

    private void fetchLoop() {
        while (running.get()) {
            try {
                List<Message> messages = subscription.fetch(
                        fetchBatchSize,
                        Duration.ofSeconds(fetchTimeoutSeconds)
                );
                if (messages.isEmpty()) {
                    continue;
                }
                for (Message message : messages) {
                    if (!running.get()) {
                        break;
                    }
                    processMessage(message);
                }
            } catch (Exception e) {
                log.error("Fetch loop error", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("TranscodeWorkflowNatsBridge fetch loop stopped");
    }

    private void processMessage(Message message) {
        Optional<TranscodeJobInput> parsed = minioEventParser.parse(message.getData());
        if (parsed.isEmpty()) {
            message.ack();
            return;
        }
        TranscodeJobInput input = parsed.get();
        String taskId = input.bucket() + "/" + input.key();
        NatsMessageHeartbeat.Handle heartbeat = natsMessageHeartbeat.start(message, taskId);
        try {
            String workflowId = "transcode-" + input.uploadId();
            VideoProcessingWorkflow stub = workflowClient.newWorkflowStub(
                    VideoProcessingWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(temporalProperties.getOrchestratorTaskQueue())
                            .setWorkflowId(workflowId)
                            .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                            .build()
            );
            try {
                WorkflowClient.start(stub::processUpload, input);
                log.info("Started workflow {} for {}", workflowId, taskId);
            } catch (WorkflowExecutionAlreadyStarted already) {
                log.info("Workflow already running {}, treating as success", workflowId);
            }
            natsMessageHeartbeat.stopAndAck(heartbeat);
        } catch (Exception e) {
            log.error("Failed to start workflow for {}: {}", taskId, e.getMessage());
            natsMessageHeartbeat.stopAndNak(heartbeat);
        }
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.warn("Unsubscribe: {}", e.getMessage());
            }
        }
        if (fetchThread != null) {
            try {
                fetchThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        natsMessageHeartbeat.shutdown();
        log.info("TranscodeWorkflowNatsBridge stopped");
    }
}
