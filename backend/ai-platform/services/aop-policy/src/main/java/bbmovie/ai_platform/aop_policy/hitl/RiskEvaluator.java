package bbmovie.ai_platform.aop_policy.hitl;

import java.lang.reflect.Method;

public interface RiskEvaluator {
    RiskLevel evaluate(Method method, Object[] args);
}
