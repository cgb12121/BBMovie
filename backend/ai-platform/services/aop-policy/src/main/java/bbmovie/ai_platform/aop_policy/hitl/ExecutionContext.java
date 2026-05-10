package bbmovie.ai_platform.aop_policy.hitl;

import bbmovie.ai_platform.aop_policy.exception.RequiresApprovalException;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ExecutionContext {
    private UUID sessionId;
    private UUID messageId;
    private UUID userId;
    private String internalApprovalToken;
    private RequiresApprovalException pendingException;
}
