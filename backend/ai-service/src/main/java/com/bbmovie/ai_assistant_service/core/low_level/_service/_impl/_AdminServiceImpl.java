package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._TokenUsageResponse;
import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._repository._AiInteractionAuditRepository;
import com.bbmovie.ai_assistant_service.core.low_level._service._AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class _AdminServiceImpl implements _AdminService {

    private final _AiInteractionAuditRepository auditRepository;

    @Autowired
    public _AdminServiceImpl(_AiInteractionAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public Mono<_TokenUsageResponse> getTokenUsageDashboard() {
        Mono<Long> totalPromptTokens = auditRepository.sumTotalPromptTokens();
        Mono<Long> totalResponseTokens = auditRepository.sumTotalResponseTokens();

        return Mono.zip(totalPromptTokens, totalResponseTokens)
                .map(tuple -> {
                    long promptTokens = tuple.getT1();
                    long responseTokens = tuple.getT2();
                    return _TokenUsageResponse.builder()
                            .totalPromptTokens(promptTokens)
                            .totalResponseTokens(responseTokens)
                            .totalTokens(promptTokens + responseTokens)
                            .build();
                });
    }

    @Override
    public Flux<_AiInteractionAudit> getAuditTrail(
            _InteractionType interactionType,
            UUID sessionId,
            Instant startDate,
            Instant endDate
    ) {
        return auditRepository.findByCriteria(interactionType, sessionId, startDate, endDate);
    }
}
