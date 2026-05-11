package bbmovie.ai_platform.aop_policy.aspect;

import bbmovie.ai_platform.aop_policy.annotation.CheckOwnership;
import bbmovie.ai_platform.aop_policy.service.OwnershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OwnershipAspect {

    private final OwnershipValidator ownershipValidator;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(checkOwnership)")
    public Object verifyOwnership(ProceedingJoinPoint joinPoint, CheckOwnership checkOwnership) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 1. Parse SpEL to get Resource ID
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Object result = parser.parseExpression(checkOwnership.expression()).getValue(context);
        if (!(result instanceof UUID resourceId)) {
            log.error("[Ownership] Expression did not result in a UUID: {}", checkOwnership.expression());
            return joinPoint.proceed();
        }

        // 2. Handle Reactive Flow
        Class<?> returnType = signature.getReturnType();

        if (Mono.class.isAssignableFrom(returnType)) {
            return ReactiveSecurityContextHolder.getContext()
                    .flatMap(ctx -> {
                        String userIdStr = ctx.getAuthentication().getName();
                        return ownershipValidator.checkOwnership(UUID.fromString(userIdStr), resourceId, checkOwnership.entityType());
                    })
                    .flatMap(isOwner -> {
                        if (Boolean.TRUE.equals(isOwner)) {
                            try {
                                return (Mono<?>) joinPoint.proceed();
                            } catch (Throwable e) {
                                return Mono.error(e);
                            }
                        }
                        return Mono.error(new AccessDeniedException("You do not have permission to access this resource."));
                    });
        }

        if (Flux.class.isAssignableFrom(returnType)) {
            return ReactiveSecurityContextHolder.getContext()
                    .flatMapMany(ctx -> {
                        String userIdStr = ctx.getAuthentication().getName();
                        return ownershipValidator.checkOwnership(UUID.fromString(userIdStr), resourceId, checkOwnership.entityType())
                                .flatMapMany(isOwner -> {
                                    if (Boolean.TRUE.equals(isOwner)) {
                                        try {
                                            return (Flux<?>) joinPoint.proceed();
                                        } catch (Throwable e) {
                                            return Flux.error(e);
                                        }
                                    }
                                    return Flux.error(new AccessDeniedException("You do not have permission to access this resource."));
                                });
                    });
        }

        // Fallback for non-reactive (not recommended for this project but included for completeness)
        return joinPoint.proceed();
    }
}
