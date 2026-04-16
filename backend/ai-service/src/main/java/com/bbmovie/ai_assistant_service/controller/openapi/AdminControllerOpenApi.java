package com.bbmovie.ai_assistant_service.controller.openapi;

import com.bbmovie.ai_assistant_service.dto.response.TokenUsageResponse;
import com.bbmovie.ai_assistant_service.entity.AiInteractionAudit;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.common.dtos.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "AI Admin", description = "AI admin and analytics APIs")
public interface AdminControllerOpenApi {
    @Operation(summary = "Token usage dashboard", description = "Get aggregate token usage metrics")
    Mono<ApiResponse<TokenUsageResponse>> getTokenUsageDashboard();

    @Operation(summary = "Query audit trail", description = "Query AI interaction audit records with optional filters")
    Flux<ApiResponse<AiInteractionAudit>> getAuditTrail(
            @RequestParam(required = false) InteractionType interactionType,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    );
}

