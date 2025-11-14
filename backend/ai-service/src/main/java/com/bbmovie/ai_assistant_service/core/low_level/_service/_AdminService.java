package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._TokenUsageResponse;
import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface _AdminService {

    Mono<_TokenUsageResponse> getTokenUsageDashboard();

    Flux<_AiInteractionAudit> getAuditTrail(
            _InteractionType interactionType,
            UUID sessionId,
            Instant startDate,
            Instant endDate
    );

}
