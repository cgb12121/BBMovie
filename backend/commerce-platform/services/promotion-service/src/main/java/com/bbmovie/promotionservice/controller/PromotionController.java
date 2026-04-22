package com.bbmovie.promotionservice.controller;

import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.promotionservice.controller.openapi.PromotionControllerOpenApi;
import com.bbmovie.promotionservice.dto.CouponApplyRequest;
import com.bbmovie.promotionservice.dto.PromotionEvaluationContext;
import com.bbmovie.promotionservice.service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController implements PromotionControllerOpenApi {

    private final PromotionService promotionService;

    @Autowired
    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping("/apply-coupon")
    public ResponseEntity<ApiResponse<PromotionEvaluationContext>> applyCoupon(@RequestBody CouponApplyRequest request) {
        PromotionEvaluationContext result = promotionService.applyCoupon(request);
        if (result.isEligible()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error(result.getReason()));
        }
    }

    @PostMapping("/evaluate-auto")
    public ResponseEntity<ApiResponse<List<PromotionEvaluationContext>>> evaluateAuto(@RequestBody PromotionEvaluationContext context) {
        List<PromotionEvaluationContext> results = promotionService.evaluateAutomaticPromotions(context);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
