package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._TokenUsageResponse;
import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AdminService;
import com.bbmovie.common.dtos.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/v1/admin")
public class _AdminController {

    private final _AdminService adminService;

    @Autowired
    public _AdminController(_AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/token-usage")
    public Mono<ApiResponse<_TokenUsageResponse>> getTokenUsageDashboard() {
        return adminService.getTokenUsageDashboard()
                .map(ApiResponse::success);
    }

    @GetMapping("/audit/query")
    public Flux<ApiResponse<_AiInteractionAudit>> getAuditTrail(
            @RequestParam(required = false) _InteractionType interactionType,
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return adminService.getAuditTrail(interactionType, sessionId, startDate, endDate)
                .map(ApiResponse::success);
    }
}
