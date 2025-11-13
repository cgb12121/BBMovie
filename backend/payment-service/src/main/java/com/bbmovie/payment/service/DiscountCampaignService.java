package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.DiscountCampaignCreateRequest;
import com.bbmovie.payment.dto.request.DiscountCampaignUpdateRequest;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface DiscountCampaignService {
    @Transactional
    DiscountCampaignResponse create(DiscountCampaignCreateRequest request);

    @Transactional
    DiscountCampaignResponse update(UUID id, DiscountCampaignUpdateRequest request);

    @Transactional(readOnly = true)
    DiscountCampaignResponse get(UUID id);

    @Transactional
    void delete(UUID id);

    @Transactional(readOnly = true)
    List<DiscountCampaignResponse> listAll();

    @Transactional(readOnly = true)
    List<DiscountCampaignResponse> listActiveNow();

    @Transactional(readOnly = true)
    List<DiscountCampaignResponse> listActiveNowByPlan(UUID planId);
}
