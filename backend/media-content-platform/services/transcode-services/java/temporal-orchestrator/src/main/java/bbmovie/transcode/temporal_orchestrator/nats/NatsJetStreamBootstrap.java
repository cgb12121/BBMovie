package bbmovie.transcode.temporal_orchestrator.nats;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class NatsJetStreamBootstrap {

    private final Connection natsConnection;
    private final String minioSubject;
    @Getter
    private final String streamName;
    @Getter
    private final String consumerDurable;
    private final int ackWaitMinutes;

    public NatsJetStreamBootstrap(
            Connection natsConnection,
            @Value("${nats.minio.subject:minio.events}") String minioSubject,
            @Value("${nats.stream.name:BBMOVIE}") String streamName,
            @Value("${nats.consumer.durable:temporal-orchestrator}") String consumerDurable,
            @Value("${nats.consumer.ack-wait-minutes:5}") int ackWaitMinutes) {
        this.natsConnection = natsConnection;
        this.minioSubject = minioSubject;
        this.streamName = streamName;
        this.consumerDurable = consumerDurable;
        this.ackWaitMinutes = ackWaitMinutes;
    }

    @Getter
    private JetStream jetStream;

    @Getter
    private JetStreamManagement jetStreamManagement;

    public void initialize() {
        try {
            this.jetStreamManagement = natsConnection.jetStreamManagement();
            this.jetStream = natsConnection.jetStream();
            log.info("NATS JetStream initialized for temporal-orchestrator bridge");
        } catch (IOException e) {
            throw new RuntimeException("NATS JetStream init failed", e);
        }
    }

    public void ensureStreamExists() throws Exception {
        try {
            jetStreamManagement.getStreamInfo(streamName);
            log.info("Stream '{}' already exists", streamName);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                log.info("Creating stream '{}' with subject '{}'", streamName, minioSubject);
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(minioSubject)
                        .storageType(StorageType.File)
                        .build();
                jetStreamManagement.addStream(streamConfig);
                log.info("Stream '{}' created successfully", streamName);
            } else {
                throw e;
            }
        }
    }

    public void setupConsumer(int maxAckPending) {
        try {
            boolean consumerExists = false;
            try {
                jetStreamManagement.getConsumerInfo(streamName, consumerDurable);
                consumerExists = true;
                log.info("Consumer '{}' already exists, updating config if needed", consumerDurable);
            } catch (JetStreamApiException e) {
                if (e.getErrorCode() != 404) {
                    throw e;
                }
            }

            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable(consumerDurable)
                    .filterSubject(minioSubject)
                    .ackWait(Duration.ofMinutes(ackWaitMinutes))
                    .maxAckPending(maxAckPending)
                    .deliverPolicy(consumerExists ? DeliverPolicy.All : DeliverPolicy.New)
                    .build();

            jetStreamManagement.addOrUpdateConsumer(streamName, consumerConfig);
            log.info("Consumer '{}' {} filterSubject={} maxAckPending={}",
                    consumerDurable, consumerExists ? "updated" : "created", minioSubject, maxAckPending);
        } catch (JetStreamApiException e) {
            log.error("Failed to setup consumer on stream '{}' durable='{}': {}", streamName, consumerDurable, e.getMessage());
            throw new RuntimeException(
                    "NATS JetStream consumer setup failed (stream='" + streamName + "', durable='" + consumerDurable + "')",
                    e);
        } catch (Exception e) {
            log.error("Failed to setup consumer: {}", e.getMessage());
            throw new RuntimeException("Failed to setup NATS consumer", e);
        }
    }

    public String getSubject() {
        return minioSubject;
    }

    public boolean isConnected() {
        return natsConnection != null && natsConnection.getStatus() == Connection.Status.CONNECTED;
    }
}
