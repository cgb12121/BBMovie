package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.dto.response.TokenUsageResponse;
import com.bbmovie.ai_assistant_service.entity.AiInteractionAudit;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.repository.AiInteractionAuditRepository;
import com.bbmovie.ai_assistant_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AiInteractionAuditRepository auditRepository;

    @Override
    public Mono<TokenUsageResponse> getTokenUsageDashboard() {
        Mono<Long> totalPromptTokens = auditRepository.sumTotalPromptTokens();
        Mono<Long> totalResponseTokens = auditRepository.sumTotalResponseTokens();

        return Mono.zip(totalPromptTokens, totalResponseTokens)
                .map(tuple -> {
                    long promptTokens = tuple.getT1();
                    long responseTokens = tuple.getT2();
                    return TokenUsageResponse.builder()
                            .totalPromptTokens(promptTokens)
                            .totalResponseTokens(responseTokens)
                            .totalTokens(promptTokens + responseTokens)
                            .build();
                });
    }

    @Override
    public Flux<AiInteractionAudit> getAuditTrail(
            InteractionType interactionType,
            UUID sessionId,
            Instant startDate,
            Instant endDate
    ) {
        return auditRepository.findByCriteria(interactionType, sessionId, startDate, endDate);
    }
}
