package bbmovie.ai_platform.aop_policy.exception;

import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import lombok.Getter;

@Getter
public class RequiresApprovalException extends RuntimeException {
    private final String requestId;
    private final String action;
    private final RiskLevel riskLevel;
    
    public RequiresApprovalException(String requestId, String action, RiskLevel riskLevel, String message) {
        super(message);
        this.requestId = requestId;
        this.action = action;
        this.riskLevel = riskLevel;
    }
}
