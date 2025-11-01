package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatMetrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._repository._AiInteractionAuditRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class _AuditService {

    private final _AiInteractionAuditRepository repository;
    private final _ModelSelector modelSelector;
    private final ObjectMapper objectMapper;

    @Autowired
    public _AuditService(
            _AiInteractionAuditRepository repository, _ModelSelector modelSelector,
            @Qualifier("_ObjectMapper") ObjectMapper objectMapper) {
        this.repository = repository;
        this.modelSelector = modelSelector;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> recordInteraction(UUID sessionId, _InteractionType type, Object details) {
        return recordInteraction(sessionId, type, details, null);
    }

    public Mono<Void> recordInteraction(
            UUID sessionId, _InteractionType type, Object details, _ChatMetrics metrics) {
        return Mono.fromCallable(() -> {
                    try {
                        String detailsJson = details != null
                                ? objectMapper.writeValueAsString(details)
                                : null;

                        return _AiInteractionAudit.builder()
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
                        return _AiInteractionAudit.builder()
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