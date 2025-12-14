package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.dto.request.ApprovalDecisionDto;
import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.service.ApprovalService;
import com.bbmovie.ai_assistant_service.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping(
            value = "/{sessionId}/approve/{requestId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<ChatStreamChunk>> handleApprovalDecision(
            @PathVariable UUID sessionId,
            @PathVariable String requestId,
            @RequestBody ApprovalDecisionDto decisionBody,
            @AuthenticationPrincipal Jwt jwt) {
            
        return approvalService.handleDecision(sessionId, requestId, decisionBody, jwt)
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }
}