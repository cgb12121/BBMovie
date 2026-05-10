package bbmovie.ai_platform.aop_policy.service;

import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

public interface ApprovalService {
    Mono<Boolean> validateInternalToken(String token, UUID sessionId);
    Mono<String> createRequest(String toolName, String action, RiskLevel risk, Map<String, Object> arguments, UUID userId, UUID sessionId, UUID messageId);
    Mono<String> handleDecision(String requestId, boolean approved);
}
