package com.bbmovie.fileservice.entity.cdc;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @Column("id")
    private String id;

    @Column("aggregate_type")
    private String aggregateType;

    @Column("aggregate_id")
    private String aggregateId;

    @Column("event_type")
    private String eventType;

    @Column("payload")
    private String payload;

    @Column("status")
    private OutboxStatus status;

    @Column("retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column("sent_at")
    private LocalDateTime sentAt;
}

