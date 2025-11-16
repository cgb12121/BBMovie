package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.response.TokenUsageResponse;
import com.bbmovie.ai_assistant_service.entity.AiInteractionAudit;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.service.AdminService;
import com.bbmovie.common.dtos.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard/token-usage")
    public Mono<ApiResponse<TokenUsageResponse>> getTokenUsageDashboard() {
        return adminService.getTokenUsageDashboard()
                .map(ApiResponse::success);
    }

    @GetMapping("/audit/query")
    public Flux<ApiResponse<AiInteractionAudit>> getAuditTrail(
            @RequestParam(required = false) InteractionType interactionType,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return adminService.getAuditTrail(interactionType, sessionId, startDate, endDate)
                .map(ApiResponse::success);
    }
}
