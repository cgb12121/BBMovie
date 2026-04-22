package com.bbmovie.droolsengine.service;

import com.bbmovie.droolsengine.dto.PromotionEvaluationContext;
import com.bbmovie.droolsengine.dto.StudentVerificationContext;
import com.bbmovie.droolsengine.enums.VerificationOutcome;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DroolsService {
    private static final Logger log = LoggerFactory.getLogger(DroolsService.class);

    private final KieContainer kieContainer;
    private final int autoApproveThreshold;
    private final int autoRejectThreshold;

    public DroolsService(KieContainer kieContainer, int autoApproveThreshold, int autoRejectThreshold) {
        this.kieContainer = kieContainer;
        this.autoApproveThreshold = autoApproveThreshold;
        this.autoRejectThreshold = autoRejectThreshold;
    }

    public StudentVerificationContext verifyStudent(StudentVerificationContext context) {
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(context);
            if (context.getUniversityMatch() != null) {
                kieSession.insert(context.getUniversityMatch());
            }
            kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }

        VerificationOutcome result = context.determineOutcome(autoApproveThreshold, autoRejectThreshold);
        log.debug("Student verification outcome: {}", result);
        return context;
    }

    public PromotionEvaluationContext evaluatePromotion(PromotionEvaluationContext context) {
        KieSession kieSession = kieContainer.newKieSession();
        try {
            kieSession.insert(context);
            kieSession.fireAllRules();
        } finally {
            kieSession.dispose();
        }
        return context;
    }
}
