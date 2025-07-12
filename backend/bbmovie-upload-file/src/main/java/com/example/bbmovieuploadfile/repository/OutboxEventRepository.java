package com.example.bbmovieuploadfile.repository;

import com.example.bbmovieuploadfile.entity.cdc.OutboxStatus;
import com.example.bbmovieuploadfile.entity.cdc.OutboxEvent;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, String> {
    Flux<OutboxEvent> findByStatus(OutboxStatus status);

    @Modifying
    Mono<Void> deleteAllByStatus(OutboxStatus status);

    @SuppressWarnings({ "squid:S00107", "unused" })
    @Query("""
        INSERT INTO outbox_events (
            id, aggregate_type, aggregate_id, event_type, payload, status,
            retry_count, created_at, last_attempt_at, sent_at
        ) VALUES (
            :id, :aggregateType, :aggregateId, :eventType, :payload, :status,
            :retryCount, :createdAt, :lastAttemptAt, :sentAt
        )
    """)
    @Modifying
    Mono<Void> insertOutboxEvent(
            @Param("id") String id,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("status") OutboxStatus status,
            @Param("retryCount") int retryCount,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("lastAttemptAt") LocalDateTime lastAttemptAt,
            @Param("sentAt") LocalDateTime sentAt
    );

    @Query("""
        INSERT INTO outbox_events (
            id, aggregate_type, aggregate_id, event_type, payload,
            status,retry_count, created_at, last_attempt_at, sent_at
        ) VALUES (
            :#{#event.id}, :#{#event.aggregateType}, :#{#event.aggregateId},:#{#event.eventType}, :#{#event.payload},
            :#{#event.status},:#{#event.retryCount}, :#{#event.createdAt},:#{#event.lastAttemptAt}, :#{#event.sentAt}
        )
    """)
    @Modifying
    Mono<Void> insertOutboxEvent(@Param("event") OutboxEvent event);

    @Modifying
    @Query("""
        UPDATE outbox_events
        SET status = :#{#event.status},
            sent_at = :#{#event.sentAt},
            last_attempt_at = :#{#event.lastAttemptAt}
        WHERE id = :#{#event.id}
    """)
    Mono<Void> updateOutboxEvent(@Param("event") OutboxEvent event);
}