package com.bbmovie.ai_assistant_service.core.low_level._dto;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class _AuditRecord {
    private UUID sessionId;
    private _InteractionType type;
    private Object details;
    private _Metrics metrics;
}
