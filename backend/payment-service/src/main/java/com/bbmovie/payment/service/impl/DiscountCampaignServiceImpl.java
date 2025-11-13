package com.bbmovie.payment.service.impl;

import com.bbmovie.payment.dto.request.DiscountCampaignCreateRequest;
import com.bbmovie.payment.dto.request.DiscountCampaignUpdateRequest;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import com.bbmovie.payment.entity.DiscountCampaign;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.exception.CampaignNotFoundException;
import com.bbmovie.payment.exception.SubscriptionPlanNotFoundException;
import com.bbmovie.payment.repository.DiscountCampaignRepository;
import com.bbmovie.payment.repository.SubscriptionPlanRepository;
import com.bbmovie.payment.service.DiscountCampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DiscountCampaignServiceImpl implements DiscountCampaignService {

    private final DiscountCampaignRepository campaignRepository;
    private final SubscriptionPlanRepository planRepository;

    @Autowired
    public DiscountCampaignServiceImpl(
            DiscountCampaignRepository campaignRepository,
            SubscriptionPlanRepository planRepository
    ) {
        this.campaignRepository = campaignRepository;
        this.planRepository = planRepository;
    }

    @Transactional
    @Override
    public DiscountCampaignResponse create(DiscountCampaignCreateRequest request) {
        SubscriptionPlan plan = planRepository.findById(UUID.fromString(request.planId()))
                .orElseThrow(SubscriptionPlanNotFoundException::new);

        DiscountCampaign entity = DiscountCampaign.builder()
                .name(request.name())
                .plan(plan)
                .discountPercent(request.discountPercent())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .active(Boolean.TRUE.equals(request.active()))
                .build();

        entity = campaignRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    @Override
    public DiscountCampaignResponse update(UUID id, DiscountCampaignUpdateRequest request) {
        DiscountCampaign entity = campaignRepository.findById(id)
                .orElseThrow(CampaignNotFoundException::new);
        SubscriptionPlan plan = planRepository.findById(UUID.fromString(request.planId()))
                .orElseThrow(SubscriptionPlanNotFoundException::new);

        entity.setName(request.name());
        entity.setPlan(plan);
        entity.setDiscountPercent(request.discountPercent());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setActive(Boolean.TRUE.equals(request.active()));

        entity = campaignRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    @Override
    public DiscountCampaignResponse get(UUID id) {
        return campaignRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(CampaignNotFoundException::new);
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        if (!campaignRepository.existsById(id)) {
            throw new CampaignNotFoundException();
        }
        campaignRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DiscountCampaignResponse> listAll() {
        return campaignRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<DiscountCampaignResponse> listActiveNow() {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository
                .findByActiveIsTrueAndStartAtBeforeAndEndAtAfter(now, now)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<DiscountCampaignResponse> listActiveNowByPlan(UUID planId) {
        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(SubscriptionPlanNotFoundException::new);
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository
                .findByPlanAndActiveIsTrueAndStartAtBeforeAndEndAtAfter(plan, now, now)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DiscountCampaignResponse toResponse(DiscountCampaign c) {
        return DiscountCampaignResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .planId(c.getPlan() != null ? c.getPlan().getId() : null)
                .discountPercent(c.getDiscountPercent())
                .startAt(c.getStartAt())
                .endAt(c.getEndAt())
                .active(c.isActive())
                .build();
    }
}


