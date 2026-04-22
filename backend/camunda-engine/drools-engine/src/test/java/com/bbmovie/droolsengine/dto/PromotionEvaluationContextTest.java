package com.bbmovie.droolsengine.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PromotionEvaluationContextTest {

    @Test
    void gettersAndSettersWork() {
        PromotionEvaluationContext context = new PromotionEvaluationContext();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        context.setUserId(userId);
        context.setUserRole("STUDENT");
        context.setUserRegion("VN");
        context.setCartValue(100.0);
        context.setCurrency("USD");
        context.setCurrentDateTime(now);
        context.setPromotionId("P1");
        context.setDroolsRuleId("RULE_1");
        context.setEligible(true);
        context.setAppliedDiscountValue(10.0);
        context.setAppliedTrialDays(14);
        context.setReason("ok");

        assertEquals(userId, context.getUserId());
        assertEquals("STUDENT", context.getUserRole());
        assertEquals("VN", context.getUserRegion());
        assertEquals(100.0, context.getCartValue());
        assertEquals("USD", context.getCurrency());
        assertEquals(now, context.getCurrentDateTime());
        assertEquals("P1", context.getPromotionId());
        assertEquals("RULE_1", context.getDroolsRuleId());
        assertEquals(10.0, context.getAppliedDiscountValue());
        assertEquals(14, context.getAppliedTrialDays());
        assertEquals("ok", context.getReason());
        assertEquals(true, context.isEligible());
    }

    @Test
    void defaultsAreFalseOrNull() {
        PromotionEvaluationContext context = new PromotionEvaluationContext();
        assertFalse(context.isEligible());
    }
}
