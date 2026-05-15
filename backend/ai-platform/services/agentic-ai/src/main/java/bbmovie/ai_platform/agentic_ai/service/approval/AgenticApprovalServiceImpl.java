package bbmovie.ai_platform.agentic_ai.service.approval;

import bbmovie.ai_platform.agentic_ai.dto.request.ApprovalDecisionDto;
import bbmovie.ai_platform.agentic_ai.entity.ApprovalRequest;
import bbmovie.ai_platform.agentic_ai.repository.ApprovalRepository;
import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * AgenticApprovalServiceImpl manages the Human-In-The-Loop (HITL) workflow for tool execution.
 * 
 * When an AI agent attempts to execute a high-risk tool, this service intercepts the call,
 * persists an approval request, and waits for a human decision.
 * 
 * Key mechanisms:
 * 1. Request Persistence: Stores tool metadata and arguments in a database for audit and review.
 * 2. Token-based Execution: Once approved, a short-lived token is stored in Redis. The agent
 *    must present this token to finally execute the protected tool.
 * 3. TTL Security: Approval tokens are session-scoped and expire automatically.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticApprovalServiceImpl implements AgenticApprovalService {

    private final ApprovalRepository approvalRepository;
    @Qualifier("rRedisTemplate") private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_PREFIX = "hitl:token:";

    /**
     * Entry point for processing a human's decision on a pending request.
     * 
     * @param sessionId The active chat session ID.
     * @param requestId The unique ID of the approval request.
     * @param decision The decision (approved/rejected).
     * @param userId The ID of the user making the decision.
     * @return A Flux emitting the result of the decision (e.g., the generated token if approved).
     */
    @Override
    public Flux<String> handleApprovalDecision(UUID sessionId, String requestId, ApprovalDecisionDto decision, UUID userId) {
        return handleDecision(requestId, decision.approved()).flux();
    }

    /**
     * Validates if a provided token matches the approved token for a session.
     * This is called by the HITL Aspect before allowing a tool method to run.
     * 
     * @param token The token provided by the agent (usually injected into tool context).
     * @param sessionId The session ID context.
     * @return Mono<Boolean> true if valid, false otherwise.
     */
    @Override
    public Mono<Boolean> validateInternalToken(String token, UUID sessionId) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + sessionId)
                .map(token::equals)
                .defaultIfEmpty(false);
    }

    /**
     * Creates a new approval request when a tool execution is intercepted.
     * 
     * @param toolName Name of the tool being called.
     * @param action Description of the action.
     * @param risk The evaluated risk level.
     * @param arguments The arguments the agent wants to pass to the tool.
     * @param userId The owner of the session.
     * @param sessionId The active session.
     * @param messageId The message ID that triggered the tool call.
     * @return Mono<String> The generated requestId.
     */
    @Override
    public Mono<String> createRequest(
            String toolName, String action, RiskLevel risk, Map<String, Object> arguments, 
            UUID userId, UUID sessionId, UUID messageId) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        
        String argsJson;
        try {
            argsJson = objectMapper.writeValueAsString(arguments);
        } catch (Exception e) {
            argsJson = "{}";
        }

        ApprovalRequest entity = ApprovalRequest.builder()
                .requestId(requestId)
                .toolName(toolName)
                .action(action)
                .riskLevel(risk.name())
                .arguments(argsJson)
                .userId(userId)
                .sessionId(sessionId)
                .messageId(messageId)
                .status("PENDING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return approvalRepository.save(entity)
                .map(ApprovalRequest::getRequestId);
    }

    /**
     * Updates the status of an approval request and generates a temporary token if approved.
     * 
     * @param requestId The ID of the request.
     * @param approved True if approved, false if rejected.
     * @return Mono<String> "REJECTED" or the generated access token.
     */
    @Override
    public Mono<String> handleDecision(String requestId, boolean approved) {
        return approvalRepository.findById(requestId)
                .flatMap(req -> {
                    req.setStatus(approved ? "APPROVED" : "REJECTED");
                    req.setUpdatedAt(Instant.now());
                    return approvalRepository.save(req);
                })
                .flatMap(req -> {
                    if (!approved) {
                        return Mono.just("REJECTED");
                    }
                    String token = UUID.randomUUID().toString();
                    // Store token in Redis for 10 minutes to allow re-execution
                    return redisTemplate.opsForValue().set(TOKEN_PREFIX + req.getSessionId(), token, Duration.ofMinutes(10))
                            .thenReturn(token);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid Request ID")));
    }
}
