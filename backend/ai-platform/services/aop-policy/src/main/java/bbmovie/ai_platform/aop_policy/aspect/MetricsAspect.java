package bbmovie.ai_platform.aop_policy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
public class MetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricsAspect.class);

    @Around("@annotation(bbmovie.ai_platform.aop_policy.annotation.Monitored)")
    public Object recordMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        String methodName = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();

            // Handle Reactive Types (Mono/Flux)
            if (result instanceof Mono<?> mono) {
                return mono.doOnTerminate(() -> logMetrics(methodName, startTime));
            } else if (result instanceof Flux<?> flux) {
                return flux.doOnTerminate(() -> logMetrics(methodName, startTime));
            }

            // Handle Synchronous Types
            logMetrics(methodName, startTime);
            return result;
        } catch (Throwable e) {
            logMetrics(methodName, startTime);
            throw e;
        }
    }

    private void logMetrics(String methodName, long startTime) {
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        
        // Convert to Milliseconds with higher precision
        double durationMillis = (double) durationNanos / 1_000_000.0;

        log.info("[Metrics] Method {} executed in {} ms", methodName, String.format("%.3f", durationMillis));
    }
}
