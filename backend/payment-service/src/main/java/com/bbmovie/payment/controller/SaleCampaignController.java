package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import com.bbmovie.payment.service.DiscountCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sale/campaign")
public class SaleCampaignController {

    private final DiscountCampaignService campaignService;

    @Autowired
    public SaleCampaignController(DiscountCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/active")
    public ApiResponse<List<DiscountCampaignResponse>> listActive() {
        return ApiResponse.success(campaignService.listActiveNow());
    }

    @GetMapping("/active/plan/{planId}")
    public ApiResponse<List<DiscountCampaignResponse>> listActiveByPlan(@PathVariable("planId") UUID planId) {
        return ApiResponse.success(campaignService.listActiveNowByPlan(planId));
    }
}
