package com.bbmovie.ai_assistant_service.dto;

import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class AuditRecord {
    private UUID sessionId;
    private InteractionType type;
    private Object details;
    private Metrics metrics;
}
