package bbmovie.ai_platform.aop_policy.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class MetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(MetricsAspect.class);

    @Around("@annotation(bbmovie.ai_platform.aop_policy.annotation.Monitored)")
    public Object recordMetrics(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            return joinPoint.proceed();
        } finally {
            stopWatch.stop();
            log.info("[Metrics] Method {} executed in {} ms", 
                    joinPoint.getSignature().toShortString(), stopWatch.getTotalTimeMillis());
        }
    }
}
