package com.bbmovie.droolsengine.service;

import com.bbmovie.droolsengine.dto.PromotionEvaluationContext;
import com.bbmovie.droolsengine.enums.VerificationOutcome;
import com.bbmovie.droolsengine.dto.StudentVerificationContext;
import com.bbmovie.droolsengine.dto.UniversityMatch;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DroolsServiceTest {

    @Test
    void verifyStudentInsertsFactsAndDeterminesOutcome() {
        KieContainer container = mock(KieContainer.class);
        KieSession session = mock(KieSession.class);
        when(container.newKieSession()).thenReturn(session);

        DroolsService service = new DroolsService(container, 80, 30);
        StudentVerificationContext context = new StudentVerificationContext();
        context.setScore(90);
        context.setUniversityMatch(UniversityMatch.builder().matched(true).confidence(1.0).build());

        StudentVerificationContext result = service.verifyStudent(context);

        assertSame(context, result);
        assertEquals(VerificationOutcome.AUTO_APPROVE, result.getOutcome());
        verify(session, times(2)).insert(any());
        verify(session).fireAllRules();
        verify(session).dispose();
    }

    @Test
    void evaluatePromotionExecutesRulesAndReturnsContext() {
        KieContainer container = mock(KieContainer.class);
        KieSession session = mock(KieSession.class);
        when(container.newKieSession()).thenReturn(session);

        DroolsService service = new DroolsService(container, 80, 30);
        PromotionEvaluationContext context = new PromotionEvaluationContext();

        PromotionEvaluationContext result = service.evaluatePromotion(context);

        assertSame(context, result);
        verify(session).insert(context);
        verify(session).fireAllRules();
        verify(session).dispose();
    }
}
