package bbmovie.commerce.payment_orchestrator_service.infrastructure.idempotency;

import bbmovie.commerce.payment_orchestrator_service.application.port.result.IdempotencyResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    @Around("@annotation(idempotent)")
    public Object applyIdempotency(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        Object[] args = pjp.getArgs();
        String key = extractKey(args, idempotent.keyArgIndex());
        Object request = extractRequest(args, idempotent.requestArgIndex());

        try {
            IdempotencyResult<Object> result = idempotencyService.execute(
                    idempotent.operation(),
                    key,
                    request,
                    toObjectClass(idempotent.responseType()),
                    () -> proceed(pjp)
            );
            return result.value();
        } catch (JoinPointExecutionException e) {
            throw e.getCause();
        }
    }

    @SuppressWarnings("unchecked")
    private Class<Object> toObjectClass(Class<?> clazz) {
        return (Class<Object>) clazz;
    }

    private String extractKey(Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Invalid idempotency key argument index: " + index);
        }
        Object key = args[index];
        if (!(key instanceof String keyString)) {
            throw new IllegalArgumentException("Idempotency key argument must be a String");
        }
        return keyString;
    }

    private Object extractRequest(Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException("Invalid idempotency request argument index: " + index);
        }
        return args[index];
    }

    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable e) {
            throw new JoinPointExecutionException(e);
        }
    }

    private static class JoinPointExecutionException extends RuntimeException {
        JoinPointExecutionException(Throwable cause) {
            super(cause);
        }
    }
}

