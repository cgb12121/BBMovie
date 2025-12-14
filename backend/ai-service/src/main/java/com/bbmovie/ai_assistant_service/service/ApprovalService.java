package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.hitl.ActionType;
import com.bbmovie.ai_assistant_service.hitl.RiskLevel;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

import com.bbmovie.ai_assistant_service.dto.request.ApprovalDecisionDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

@Repository
public interface ApprovalService {

    Mono<String> createRequest(
            String toolName,
            ActionType actionType,
            RiskLevel riskLevel,
            Map<String, Object> args,
            String userId,
            UUID sessionId,
            String messageId);

    Mono<Boolean> validateInternalToken(String token, UUID sessionId);

    /**
     * Orchestrates the approval decision flow.
     */
    Flux<ChatStreamChunk> handleDecision(UUID sessionId, String requestId, ApprovalDecisionDto decisionBody, Jwt jwt);
}
