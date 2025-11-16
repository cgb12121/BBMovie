package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.response.TokenUsageResponse;
import com.bbmovie.ai_assistant_service.entity.AiInteractionAudit;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface AdminService {

    Mono<TokenUsageResponse> getTokenUsageDashboard();

    Flux<AiInteractionAudit> getAuditTrail(
            InteractionType interactionType,
            UUID sessionId,
            Instant startDate,
            Instant endDate
    );

}
