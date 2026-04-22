package com.bbmovie.droolsengine.controller;

import com.bbmovie.droolsengine.controller.openapi.DroolsControllerOpenApi;
import com.bbmovie.droolsengine.dto.PromotionEvaluationContext;
import com.bbmovie.droolsengine.dto.StudentVerificationContext;
import com.bbmovie.droolsengine.service.DroolsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class DroolsController implements DroolsControllerOpenApi {

    private final DroolsService droolsService;

    public DroolsController(DroolsService droolsService) {
        this.droolsService = droolsService;
    }

    @PostMapping("/verify-student")
    public StudentVerificationContext verifyStudent(@RequestBody StudentVerificationContext context) {
        return droolsService.verifyStudent(context);
    }

    @PostMapping("/evaluate-promotion")
    public PromotionEvaluationContext evaluatePromotion(@RequestBody PromotionEvaluationContext context) {
        return droolsService.evaluatePromotion(context);
    }
}
