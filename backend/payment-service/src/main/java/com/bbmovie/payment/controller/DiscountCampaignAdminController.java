package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.DiscountCampaignCreateRequest;
import com.bbmovie.payment.dto.request.DiscountCampaignUpdateRequest;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import com.bbmovie.payment.service.DiscountCampaignService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/campaigns")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class DiscountCampaignAdminController {

    private final DiscountCampaignService campaignService;

    @Autowired
    public DiscountCampaignAdminController(DiscountCampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ApiResponse<DiscountCampaignResponse> create(@Valid @RequestBody DiscountCampaignCreateRequest request) {
        return ApiResponse.success(campaignService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DiscountCampaignResponse> update(@PathVariable("id") UUID id,
                                                        @Valid @RequestBody DiscountCampaignUpdateRequest request) {
        return ApiResponse.success(campaignService.update(id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DiscountCampaignResponse> get(@PathVariable("id") UUID id) {
        return ApiResponse.success(campaignService.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") UUID id) {
        campaignService.delete(id);
        return ApiResponse.success(null, "Deleted");
    }

    @GetMapping
    public ApiResponse<List<DiscountCampaignResponse>> listAll() {
        return ApiResponse.success(campaignService.listAll());
    }
}


