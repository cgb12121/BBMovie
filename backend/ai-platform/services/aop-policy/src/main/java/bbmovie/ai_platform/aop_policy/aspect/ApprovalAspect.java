package bbmovie.ai_platform.aop_policy.aspect;

import bbmovie.ai_platform.aop_policy.exception.RequiresApprovalException;
import bbmovie.ai_platform.aop_policy.hitl.ApprovalContextHolder;
import bbmovie.ai_platform.aop_policy.hitl.ExecutionContext;
import bbmovie.ai_platform.aop_policy.hitl.RequiresApproval;
import bbmovie.ai_platform.aop_policy.hitl.RiskEvaluator;
import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import bbmovie.ai_platform.aop_policy.service.ApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
public class ApprovalAspect {

    private final RiskEvaluator riskEvaluator;
    private final ApprovalService approvalService;

    public ApprovalAspect(RiskEvaluator riskEvaluator, ApprovalService approvalService) {
        this.riskEvaluator = riskEvaluator;
        this.approvalService = approvalService;
    }

    @Around("@annotation(requiresApproval)")
    public Object enforceApproval(ProceedingJoinPoint joinPoint, RequiresApproval requiresApproval) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        ExecutionContext context = ApprovalContextHolder.get();
        if (context == null) {
             return joinPoint.proceed(); 
        }

        RiskLevel currentRisk = riskEvaluator.evaluate(method, args, context.getUserRole());

        if (currentRisk.ordinal() >= requiresApproval.riskLevel().ordinal()) {

            boolean isApproved = false;
            if (context.getInternalApprovalToken() != null) {
                isApproved = Boolean.TRUE.equals(approvalService.validateInternalToken(
                        context.getInternalApprovalToken(),
                        context.getSessionId()
                ).block()); 
            }

            if (!isApproved) {
                Map<String, Object> argMap = toMap(args);
                String requestId = approvalService.createRequest(
                        method.getName(),
                        requiresApproval.action(),
                        currentRisk,
                        argMap,
                        context.getUserId(),
                        context.getSessionId(),
                        context.getMessageId()
                ).block();

                RequiresApprovalException ex = new RequiresApprovalException(
                        requestId,
                        requiresApproval.action(),
                        currentRisk,
                        requiresApproval.description()
                );
                
                context.setPendingException(ex);
                
                log.debug("[HITL_DEBUG] Aspect set PendingException {} on Context {}", ex.getRequestId(), System.identityHashCode(context));
                
                return "Approval Required. Request ID: " + requestId; 
            }
        }

        return joinPoint.proceed();
    }

    private Map<String, Object> toMap(Object[] args) {
        Map<String, Object> map = new HashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                map.put("arg" + i, args[i]);
            }
        }
        return map;
    }
}
