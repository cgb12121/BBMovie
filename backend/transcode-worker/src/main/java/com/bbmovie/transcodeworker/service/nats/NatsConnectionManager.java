package com.bbmovie.transcodeworker.service.nats;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Manages NATS JetStream connection, stream, and consumer setup.
 * <p>
 * Extracted from MediaEventConsumer to follow the Single Responsibility Principle.
 * This class handles all NATS connection and configuration concerns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NatsConnectionManager {

    private final Connection natsConnection;

    @Value("${nats.minio.subject:minio.events}")
    private String minioSubject;

    /**
     * -- GETTER --
     *  Returns the stream name.
     */
    @Getter
    @Value("${nats.stream.name:BBMOVIE}")
    private String streamName;

    /**
     * -- GETTER --
     *  Returns the consumer durable name.
     */
    @Getter
    @Value("${nats.consumer.durable:transcode-worker}")
    private String consumerDurable;

    @Value("${nats.consumer.ack-wait-minutes:5}")
    private int ackWaitMinutes;

    @Getter
    private JetStream jetStream;

    @Getter
    private JetStreamManagement jetStreamManagement;

    /**
     * Initializes JetStream and JetStreamManagement instances.
     * Should be called before using other methods.
     */
    public void initialize() throws Exception {
        this.jetStreamManagement = natsConnection.jetStreamManagement();
        this.jetStream = natsConnection.jetStream();
        log.info("NATS JetStream initialized");
    }

    /**
     * Ensures the stream exists, creates it if not.
     */
    public void ensureStreamExists() throws Exception {
        try {
            StreamInfo streamInfo = jetStreamManagement.getStreamInfo(streamName);
            log.info("Stream '{}' already exists. {}", streamName, streamInfo.getClusterInfo().getName());
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

    /**
     * Sets up or updates the consumer with specified max_ack_pending.
     * <p>
     * IMPORTANT: Do NOT delete existing consumer! Deleting consumer loses ACK position,
     * causing all messages to be redelivered from the beginning of the stream.
     *
     * @param maxAckPending Maximum number of unacknowledged messages
     */
    public void setupConsumer(int maxAckPending) {
        try {
            // Check if consumer already exists
            boolean consumerExists = false;
            try {
                jetStreamManagement.getConsumerInfo(streamName, consumerDurable);
                consumerExists = true;
                log.info("Consumer '{}' already exists, updating config if needed", consumerDurable);
            } catch (JetStreamApiException e) {
                if (e.getErrorCode() == 404) {
                    log.info("Consumer '{}' doesn't exist, will create new", consumerDurable);
                }
            }

            // Build consumer config - use DeliverPolicy.New for new consumers to skip old messages
            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable(consumerDurable)
                    .filterSubject(minioSubject)  // CRITICAL: Must match subscribe subject
                    .ackWait(Duration.ofMinutes(ackWaitMinutes))
                    .maxAckPending(maxAckPending)
                    // DeliverPolicy.All for existing (resume from last ACK position)
                    // DeliverPolicy.New for new (only new messages, skip historical)
                    .deliverPolicy(consumerExists ? DeliverPolicy.All : DeliverPolicy.New)
                    .build();

            jetStreamManagement.addOrUpdateConsumer(streamName, consumerConfig);
            log.info("Consumer '{}' {} with filterSubject={}, max_ack_pending={}, ack_wait={} minutes, deliverPolicy={}",
                    consumerDurable, consumerExists ? "updated" : "created",
                    minioSubject, maxAckPending, ackWaitMinutes,
                    consumerExists ? "All (resume)" : "New (skip old)");
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                log.warn("Stream '{}' not found when setting up consumer", streamName);
            } else {
                log.error("Failed to setup consumer: {}", e.getMessage());
                throw new RuntimeException("Failed to setup NATS consumer", e);
            }
        } catch (Exception e) {
            log.error("Failed to setup consumer: {}", e.getMessage());
            throw new RuntimeException("Failed to setup NATS consumer", e);
        }
    }

    /**
     * Returns the NATS connection for direct operations.
     */
    public Connection getConnection() {
        return natsConnection;
    }

    /**
     * Returns the subject being subscribed to.
     */
    public String getSubject() {
        return minioSubject;
    }

    /**
     * Checks if the NATS connection is active.
     */
    public boolean isConnected() {
        return natsConnection != null &&
                natsConnection.getStatus() == Connection.Status.CONNECTED;
    }
}

