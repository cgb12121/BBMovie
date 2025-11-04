package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface  _AuditService {
    Mono<Void> recordInteraction(UUID sessionId, _InteractionType type, Object details, _Metrics metrics);
}
