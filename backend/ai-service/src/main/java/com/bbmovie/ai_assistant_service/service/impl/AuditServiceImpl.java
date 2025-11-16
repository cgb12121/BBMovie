package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.config.ai.ModelSelector;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.entity.AiInteractionAudit;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.repository.AiInteractionAuditRepository;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AiInteractionAuditRepository repository;
    private final ModelSelector modelSelector;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditServiceImpl(
            AiInteractionAuditRepository repository, ModelSelector modelSelector,
            @Qualifier("ObjectMapper") ObjectMapper objectMapper) {
        this.repository = repository;
        this.modelSelector = modelSelector;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> recordInteraction(AuditRecord record) {
        UUID sessionId = record.getSessionId();
        InteractionType type = record.getType();
        Object details = record.getDetails();
        Metrics metrics = record.getMetrics();

        return Mono.fromCallable(() -> {
                    try {
                        String detailsJson = details != null
                                ? objectMapper.writeValueAsString(details)
                                : null;

                        return AiInteractionAudit.builder()
                                .sessionId(sessionId)
                                .interactionType(type)
                                .modelName(modelSelector.getModelName())
                                .latencyMs(metrics != null ? metrics.getLatencyMs() : null)
                                .promptTokens(metrics != null ? metrics.getPromptTokens() : null)
                                .responseTokens(metrics != null ? metrics.getResponseTokens() : null)
                                .timestamp(Instant.now())
                                .details(detailsJson)
                                .build();
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize audit details", e);
                        // Return audit without details rather than failing completely
                        return AiInteractionAudit.builder()
                                .sessionId(sessionId)
                                .interactionType(type)
                                .modelName(modelSelector.getModelName())
                                .timestamp(Instant.now())
                                .details("Error serializing details: " + e.getMessage())
                                .build();
                    }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(repository::save)
        .doOnSuccess(saved ->
                log.debug("Audit saved: type={}, session={}, id={}", type, sessionId, saved.getId()))
        .doOnError(e ->
                log.error("Failed to save audit: type={}, session={}", type, sessionId, e))
        .onErrorResume(e -> {
            // Don't fail the main flow if audit fails
            log.error("Swallowing audit error to prevent flow disruption", e);
            return Mono.empty();
        })
        .then();
    }
}