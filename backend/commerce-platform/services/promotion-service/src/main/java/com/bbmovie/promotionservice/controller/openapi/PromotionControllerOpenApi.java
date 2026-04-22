package com.bbmovie.promotionservice.controller.openapi;

import com.bbmovie.common.dtos.ApiResponse;
import com.bbmovie.promotionservice.dto.CouponApplyRequest;
import com.bbmovie.promotionservice.dto.PromotionEvaluationContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Promotions", description = "Promotion evaluation APIs")
public interface PromotionControllerOpenApi {
    @Operation(summary = "Apply coupon")
    ResponseEntity<ApiResponse<PromotionEvaluationContext>> applyCoupon(@RequestBody CouponApplyRequest request);

    @Operation(summary = "Evaluate automatic promotions")
    ResponseEntity<ApiResponse<List<PromotionEvaluationContext>>> evaluateAuto(@RequestBody PromotionEvaluationContext context);
}

