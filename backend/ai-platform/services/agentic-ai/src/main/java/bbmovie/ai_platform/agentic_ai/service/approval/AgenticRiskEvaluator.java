package bbmovie.ai_platform.agentic_ai.service.approval;

import bbmovie.ai_platform.aop_policy.hitl.RiskEvaluator;
import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Slf4j
@Service
public class AgenticRiskEvaluator implements RiskEvaluator {

    @Override
    public RiskLevel evaluate(Method method, Object[] args) {
        // Logic đánh giá rủi ro tập trung tại Agentic AI
        // Ví dụ: kiểm tra annotation @Risk hoặc dựa trên tên method/arguments
        log.info("Evaluating risk for method: {}", method.getName());
        
        // Mặc định trả về LOW hoặc logic tùy chỉnh
        if (method.getName().toLowerCase().contains("delete") || 
            method.getName().toLowerCase().contains("remove")) {
            return RiskLevel.HIGH;
        }
        
        return RiskLevel.LOW;
    }
}
