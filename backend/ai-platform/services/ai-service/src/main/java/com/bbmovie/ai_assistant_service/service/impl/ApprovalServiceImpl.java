package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.entity.ApprovalRequest;
import com.bbmovie.ai_assistant_service.hitl.ActionType;
import com.bbmovie.ai_assistant_service.hitl.RiskLevel;
import com.bbmovie.ai_assistant_service.repository.ApprovalRequestRepository;
import com.bbmovie.ai_assistant_service.service.ApprovalService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import com.bbmovie.ai_assistant_service.exception.ServiceUnavailableException;
import java.util.Objects;
import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;
import com.bbmovie.ai_assistant_service.dto.request.ApprovalDecisionDto;
import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.service.ChatService;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ApprovalServiceImpl.class);

    private final ApprovalRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<ChatService> chatServiceObjectProvider;

    @Override
    public Flux<ChatStreamChunk> handleDecision(UUID sessionId, String requestId, ApprovalDecisionDto decisionBody, Jwt jwt) {
        String userId = jwt.getClaimAsString(SUB);
        String role = jwt.getClaimAsString(ROLE);
        AssistantType assistantForYou = AssistantType.fromUserRole(role);

        Mono<ChatRequestDto> requestMono;

        if (decisionBody.decision() == ApprovalDecisionDto.Decision.REJECT) {
            requestMono = createRejectionRequest(requestId, userId, assistantForYou.name());
        } else {
            requestMono = createApproveRequest(requestId, userId, assistantForYou.name());
        }

        ChatService chatService = chatServiceObjectProvider.getIfAvailable();
        if (Objects.isNull(chatService)) {
            log.error("ChatService is not available, cannot process approval decision.");
            return Flux.error(new ServiceUnavailableException("Chat service is temporarily unavailable. Please try again later."));
        }

        return requestMono
                .flatMapMany(chatRequest -> chatService.chat(sessionId, chatRequest, jwt));
    }
    
    // ... existing implementations ...

    @Override
    public Mono<String> createRequest(
            String toolName, ActionType actionType, RiskLevel riskLevel, Map<String, Object> args,
            String userId, UUID sessionId, String messageId) {

        String requestId = UuidCreator.getTimeOrderedEpoch().toString();
        String internalToken = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plusMinutes(5);
        String payloadJson = toJson(args);

        ApprovalRequest request = ApprovalRequest.builder()
                .id(requestId)
                .approvalToken(internalToken)
                .userId(userId)
                .sessionId(sessionId.toString())
                .messageId(messageId)
                .actionType(actionType.name())
                .riskLevel(riskLevel.name())
                .toolName(toolName)
                .payload(payloadJson)
                .status(ApprovalRequest.ApprovalStatus.PENDING.name())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        return repository.createRequest(request)
                .doOnSuccess(count -> log.info("Inserted approval request {} (Rows: {})", requestId, count))
                .thenReturn(requestId);
    }

    private Mono<ApprovalRequest> approve(String requestId, String userId) {
        return repository.findById(requestId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Request not found")))
                .flatMap(req -> {
                    if (!req.getUserId().equals(userId)) {
                        return Mono.error(new SecurityException("Mismatched user binding"));
                    }
                    if (!ApprovalRequest.ApprovalStatus.PENDING.name().equals(req.getStatus())) {
                        return Mono.error(new IllegalStateException("Request not pending"));
                    }
                    
                    req.setStatus(ApprovalRequest.ApprovalStatus.APPROVED.name());
                    req.setApprovedAt(LocalDateTime.now(ZoneOffset.UTC));
                    return repository.save(req);
                })
                .doOnSuccess(req -> log.info("Request {} approved by user {}", requestId, userId));
    }

    private Mono<Void> reject(String requestId, String userId) {
        return repository.findById(requestId)
                .filter(req -> req.getUserId().equals(userId) && ApprovalRequest.ApprovalStatus.PENDING.name().equals(req.getStatus()))
                .flatMap(req -> {
                    req.setStatus(ApprovalRequest.ApprovalStatus.REJECTED.name());
                    return repository.save(req);
                })
                .doOnSuccess(v -> log.info("Request {} rejected by user {}", requestId, userId))
                .then();
    }

    private Mono<ChatRequestDto> createRejectionRequest(String requestId, String userId, String assistantType) {
        return reject(requestId, userId)
                .thenReturn(ChatRequestDto.builder()
                        .message("User rejected approval " + requestId + ". Confirm cancellation.")
                        .aiMode(AiMode.THINKING)
                        .assistantType(assistantType)
                        .build());
    }

    private Mono<ChatRequestDto> createApproveRequest(String requestId, String userId, String assistantType) {
        return approve(requestId, userId)
                .map(request -> ChatRequestDto.builder()
                        .message("Execute approved action " + request.getToolName())
                        .aiMode(AiMode.THINKING)
                        .assistantType(assistantType)
                        .internalApprovalToken(request.getApprovalToken())
                        .build());
    }

    @Override
    @Transactional
    public Mono<Boolean> validateInternalToken(String token, UUID sessionId) {
        return repository.findByApprovalTokenAndSessionId(token, sessionId.toString())
                .flatMap(req -> {
                    boolean isApproved = ApprovalRequest.ApprovalStatus.APPROVED.name().equals(req.getStatus());
                    boolean isNotExpired = req.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC));

                    if (isApproved && isNotExpired) {
                        // Consume the token to prevent replay
                        req.setStatus(ApprovalRequest.ApprovalStatus.EXECUTED.name());
                        return repository.save(req).thenReturn(true);
                    }
                    
                    return Mono.just(false);
                })
                .defaultIfEmpty(false);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}