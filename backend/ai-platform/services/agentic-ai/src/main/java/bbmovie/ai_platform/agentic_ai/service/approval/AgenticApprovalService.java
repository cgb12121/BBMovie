package bbmovie.ai_platform.agentic_ai.service.approval;

import java.util.UUID;

import bbmovie.ai_platform.agentic_ai.dto.request.ApprovalDecisionDto;
import bbmovie.ai_platform.aop_policy.service.ApprovalService;
import reactor.core.publisher.Flux;

public interface AgenticApprovalService extends ApprovalService {
     Flux<String> handleApprovalDecision(UUID sessionId, String requestId, ApprovalDecisionDto decisionBody, UUID mockUserId);
}
