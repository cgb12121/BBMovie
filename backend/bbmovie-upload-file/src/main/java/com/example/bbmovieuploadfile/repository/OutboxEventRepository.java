package com.example.bbmovieuploadfile.repository;

import com.example.bbmovieuploadfile.entity.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, String> {
    Flux<OutboxEvent> findByStatus(OutboxStatus status);

    Mono<Void> deleteAllByStatus(OutboxStatus status);

    @SuppressWarnings("squid:S00107")
    @Query("""
        INSERT INTO outbox_events (
            id, aggregate_type, aggregate_id, event_type, payload, status,
            retry_count, created_at, last_attempt_at, sent_at
        ) VALUES (
            :id, :aggregateType, :aggregateId, :eventType, :payload, :status,
            :retryCount, :createdAt, :lastAttemptAt, :sentAt
        )
        """)
    Mono<Void> insertOutboxEvent(
            String id,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payload,
            OutboxStatus status,
            int retryCount,
            LocalDateTime createdAt,
            LocalDateTime lastAttemptAt,
            LocalDateTime sentAt
    );
}