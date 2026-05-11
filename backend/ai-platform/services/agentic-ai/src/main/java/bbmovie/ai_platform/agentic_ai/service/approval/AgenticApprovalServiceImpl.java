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

@Slf4j
@Service
@RequiredArgsConstructor
public class AgenticApprovalServiceImpl implements AgenticApprovalService {

    private final ApprovalRepository approvalRepository;
    @Qualifier("rRedisTemplate") private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOKEN_PREFIX = "hitl:token:";

    // Facade for the controller
    @Override
    public Flux<String> handleApprovalDecision(UUID sessionId, String requestId, ApprovalDecisionDto decision, UUID userId) {
        return handleDecision(requestId, decision.approved()).flux();
    }

    @Override
    public Mono<Boolean> validateInternalToken(String token, UUID sessionId) {
        return redisTemplate.opsForValue().get(TOKEN_PREFIX + sessionId)
                .map(token::equals)
                .defaultIfEmpty(false);
    }

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
                    // Store token in Redis for 10 minutes
                    return redisTemplate.opsForValue().set(TOKEN_PREFIX + req.getSessionId(), token, Duration.ofMinutes(10))
                            .thenReturn(token);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid Request ID")));
    }
}
