package bbmovie.ai_platform.aop_policy.hitl;

import java.lang.reflect.Method;

public interface RiskEvaluator {
    /**
     * Evaluates whether a tool call should be intercepted for human approval.
     *
     * @param method   the tool method being invoked
     * @param args     the method arguments
     * @param userRole the caller's role extracted from JWT (e.g. "ADMIN", "USER")
     * @return the evaluated risk level
     * @throws bbmovie.ai_platform.aop_policy.exception.AccessDeniedException
     *         if the caller's role is not in {@code @RequiresApproval.requiredRoles()}
     */
    RiskLevel evaluate(Method method, Object[] args, String userRole);
}
