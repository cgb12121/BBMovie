package com.bbmovie.ai_assistant_service.entity;

import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Table("ai_interaction_audit")
public class AiInteractionAudit {

    @Id
    private Long id;

    @Column("session_id")
    private UUID sessionId;

    @Column("interaction_type")
    private InteractionType interactionType;

    @Column("timestamp")
    private Instant timestamp;

    @Column("model_name")
    private String modelName;

    @Column("latency_ms")
    private Long latencyMs;

    @Column("prompt_tokens")
    private Integer promptTokens;

    @Column("response_tokens")
    private Integer responseTokens;

    @Column("details")
    private String details; // Storing complex data as a JSON string
}
