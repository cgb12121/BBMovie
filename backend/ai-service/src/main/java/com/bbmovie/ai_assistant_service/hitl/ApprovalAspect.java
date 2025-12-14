package com.bbmovie.ai_assistant_service.hitl;

import com.bbmovie.ai_assistant_service.exception.RequiresApprovalException;
import com.bbmovie.ai_assistant_service.service.ApprovalService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class ApprovalAspect {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ApprovalAspect.class);

    private final RiskEvaluator riskEvaluator;
    private final ApprovalService approvalService;

    @Around("@annotation(requiresApproval)")
    public Object enforceApproval(ProceedingJoinPoint joinPoint, RequiresApproval requiresApproval) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Get Context
        ExecutionContext context = ApprovalContextHolder.get();
        if (context == null) {
             return joinPoint.proceed(); 
        }

        // Evaluate Risk
        RiskLevel currentRisk = riskEvaluator.evaluate(method, args);

        // Check if Approval is Needed (Risk >= MEDIUM)
        if (currentRisk.ordinal() >= RiskLevel.MEDIUM.ordinal()) {

            // Verify Internal Token
            boolean isApproved = false;
            if (context.getInternalApprovalToken() != null) {
                isApproved = Boolean.TRUE.equals(approvalService.validateInternalToken(
                        context.getInternalApprovalToken(),
                        context.getSessionId()
                ).block()); 
            }

            if (!isApproved) {
                // Create Request & Block
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
                
                // FORCE LOG
                log.debug("[HITL_DEBUG] Aspect set PendingException {} on Context {}", ex.getRequestId(), System.identityHashCode(context));
                
                return "Approval Required"; 
            }
        }

        // Proceed if Safe or Approved
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
