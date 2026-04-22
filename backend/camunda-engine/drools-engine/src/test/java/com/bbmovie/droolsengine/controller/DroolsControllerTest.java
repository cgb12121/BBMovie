package com.bbmovie.droolsengine.controller;

import com.bbmovie.droolsengine.dto.PromotionEvaluationContext;
import com.bbmovie.droolsengine.dto.StudentVerificationContext;
import com.bbmovie.droolsengine.service.DroolsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DroolsControllerTest {

    @Test
    void verifyStudentDelegatesToService() {
        DroolsService service = mock(DroolsService.class);
        DroolsController controller = new DroolsController(service);
        StudentVerificationContext context = new StudentVerificationContext();
        when(service.verifyStudent(context)).thenReturn(context);

        StudentVerificationContext result = controller.verifyStudent(context);

        assertSame(context, result);
        verify(service).verifyStudent(context);
    }

    @Test
    void evaluatePromotionDelegatesToService() {
        DroolsService service = mock(DroolsService.class);
        DroolsController controller = new DroolsController(service);
        PromotionEvaluationContext context = new PromotionEvaluationContext();
        when(service.evaluatePromotion(context)).thenReturn(context);

        PromotionEvaluationContext result = controller.evaluatePromotion(context);

        assertSame(context, result);
        verify(service).evaluatePromotion(context);
    }
}
