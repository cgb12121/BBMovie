package com.bbmovie.droolsengine.controller.openapi;

import com.bbmovie.droolsengine.dto.PromotionEvaluationContext;
import com.bbmovie.droolsengine.dto.StudentVerificationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

@SuppressWarnings("unused")
@Tag(name = "Drools Rules", description = "Rule evaluation APIs")
public interface DroolsControllerOpenApi {
    @Operation(summary = "Verify student with rules")
    StudentVerificationContext verifyStudent(@RequestBody StudentVerificationContext context);

    @Operation(summary = "Evaluate promotion with rules")
    PromotionEvaluationContext evaluatePromotion(@RequestBody PromotionEvaluationContext context);
}

