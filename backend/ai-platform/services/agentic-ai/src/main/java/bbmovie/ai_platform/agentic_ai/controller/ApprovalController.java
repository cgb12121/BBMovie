package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.ApprovalDecisionDto;
import bbmovie.ai_platform.agentic_ai.service.ApprovalService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping(
            value = "/{sessionId}/approve/{requestId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> handleApprovalDecision(
            @PathVariable UUID sessionId,
            @PathVariable String requestId,
            @RequestBody ApprovalDecisionDto decisionBody) {
        UUID mockUserId = UUID.randomUUID(); // Placeholder for Auth Context
        return approvalService.handleDecision(sessionId, requestId, decisionBody, mockUserId);
    }
}
