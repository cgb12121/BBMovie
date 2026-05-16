package bbmovie.ai_platform.agentic_ai.service.approval;

import bbmovie.ai_platform.aop_policy.hitl.RequiresApproval;
import bbmovie.ai_platform.aop_policy.hitl.RiskEvaluator;
import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Agentic-AI implementation of {@link RiskEvaluator}.
 *
 * <p><b>Risk evaluation:</b> reads {@link RiskLevel} directly from the
 * {@link RequiresApproval} annotation — no fragile method-name heuristics.
 *
 * <p><b>Role enforcement:</b> if {@code @RequiresApproval.requiredRoles()} is non-empty,
 * throws {@link ResponseStatusException} (403) when the caller's role is not in the list.
 * This acts as a hard gate — the tool is simply not executable by unauthorized roles,
 * regardless of whether the circuit breaker / HITL flow would have approved it.
 *
 * <p>Unannotated methods return {@link RiskLevel#LOW} and pass role check unconditionally.
 */
@Slf4j
@Service
public class AgenticRiskEvaluator implements RiskEvaluator {

    @Override
    public RiskLevel evaluate(Method method, Object[] args, String userRole) {
        RequiresApproval annotation = method.getAnnotation(RequiresApproval.class);

        // Unannotated methods are safe to run without approval
        if (annotation == null) {
            return RiskLevel.LOW;
        }

        // 1. Role check — hard gate before risk evaluation
        String[] requiredRoles = annotation.requiredRoles();
        if (requiredRoles.length > 0) {
            boolean hasRole = userRole != null
                    && Arrays.asList(requiredRoles).contains(userRole);
            if (!hasRole) {
                log.warn("[HITL] Role check failed: method='{}', required={}, caller='{}'",
                        method.getName(), Arrays.toString(requiredRoles), userRole);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Insufficient role to invoke tool: " + method.getName());
            }
        }

        // 2. Return annotation-declared risk level
        log.info("[HITL] Risk evaluated: method='{}', level={}", method.getName(), annotation.riskLevel());
        return annotation.riskLevel();
    }
}

