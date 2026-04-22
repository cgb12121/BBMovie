package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Sale Campaigns", description = "Public APIs for active sale campaigns")
public interface SaleCampaignControllerOpenApi {
    @Operation(summary = "List active campaigns")
    ApiResponse<List<DiscountCampaignResponse>> listActive();

    @Operation(summary = "List active campaigns by plan")
    ApiResponse<List<DiscountCampaignResponse>> listActiveByPlan(@PathVariable("planId") UUID planId);
}

