package com.bbmovie.promotionservice.service;

import com.bbmovie.promotionservice.dto.CouponApplyRequest;
import com.bbmovie.promotionservice.dto.PromotionEvaluationContext;
import com.bbmovie.promotionservice.enums.PromotionStatus;
import com.bbmovie.promotionservice.entity.Coupon;
import com.bbmovie.promotionservice.entity.Promotion;
import com.bbmovie.promotionservice.entity.UserPromotionUsage;
import com.bbmovie.promotionservice.repository.CouponRepository;
import com.bbmovie.promotionservice.repository.PromotionRepository;
import com.bbmovie.promotionservice.repository.UserPromotionUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;
    private final RestTemplate restTemplate;

    @Value("${drools.service.url:http://localhost:8081/api/rules/evaluate-promotion}")
    private String droolsServiceUrl;

    @Autowired
    public PromotionService(PromotionRepository promotionRepository,
                            CouponRepository couponRepository,
                            UserPromotionUsageRepository userPromotionUsageRepository) {
        this.promotionRepository = promotionRepository;
        this.couponRepository = couponRepository;
        this.userPromotionUsageRepository = userPromotionUsageRepository;
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public PromotionEvaluationContext applyCoupon(CouponApplyRequest request) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(request.getCode());
        if (couponOpt.isEmpty()) {
            return PromotionEvaluationContext.builder()
                    .eligible(false)
                    .reason("Invalid coupon code")
                    .build();
        }

        Coupon coupon = couponOpt.get();
        Promotion promotion = coupon.getPromotion();

        // Basic checks
        if (promotion.getStatus() != PromotionStatus.ACTIVE) {
            return PromotionEvaluationContext.builder().eligible(false).reason("Coupon is not active").build();
        }
        if (promotion.getStartDate().isAfter(LocalDateTime.now())) {
            return PromotionEvaluationContext.builder().eligible(false).reason("Coupon is not yet valid").build();
        }
        if (promotion.getEndDate().isBefore(LocalDateTime.now())) {
            return PromotionEvaluationContext.builder().eligible(false).reason("Coupon has expired").build();
        }
        if (coupon.getUsageLimit() != null && coupon.getCurrentUsage() >= coupon.getUsageLimit()) {
            return PromotionEvaluationContext.builder().eligible(false).reason("Coupon usage limit reached").build();
        }
        if (request.getCartValue() != null && coupon.getMinPurchaseAmount() != null && request.getCartValue() < coupon.getMinPurchaseAmount()) {
            return PromotionEvaluationContext.builder().eligible(false).reason("Minimum purchase amount not met").build();
        }
        if (userPromotionUsageRepository.existsByUserIdAndPromotionId(request.getUserId(), promotion.getId())) {
            return PromotionEvaluationContext.builder().eligible(false).reason("User has already used this promotion").build();
        }

        // Apply
        coupon.setCurrentUsage(coupon.getCurrentUsage() + 1);
        couponRepository.save(coupon);

        userPromotionUsageRepository.save(UserPromotionUsage.builder()
                .userId(request.getUserId())
                .promotion(promotion)
                .couponCode(coupon.getCode())
                .build());

        return PromotionEvaluationContext.builder()
                .userId(request.getUserId())
                .eligible(true)
                .appliedDiscountValue(promotion.getDiscountValue())
                .appliedTrialDays(promotion.getTrialDays())
                .reason("Coupon applied successfully: " + promotion.getName())
                .build();
    }

    public List<PromotionEvaluationContext> evaluateAutomaticPromotions(PromotionEvaluationContext context) {
        List<Promotion> autoPromotions = promotionRepository.findByStatusAndAutomaticTrueAndStartDateBeforeAndEndDateAfter(
                PromotionStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());

        List<PromotionEvaluationContext> results = new ArrayList<>();

        for (Promotion promo : autoPromotions) {
            if (userPromotionUsageRepository.existsByUserIdAndPromotionId(context.getUserId(), promo.getId())) {
                continue;
            }

            context.setPromotionId(promo.getId().toString());
            context.setDroolsRuleId(promo.getDroolsRuleId());
            context.setCurrentDateTime(LocalDateTime.now());

            try {
                PromotionEvaluationContext result = restTemplate.postForObject(droolsServiceUrl, context, PromotionEvaluationContext.class);
                if (result != null && result.isEligible()) {
                    // Inject DB values if Drools doesn't override them
                    if (result.getAppliedDiscountValue() == null) result.setAppliedDiscountValue(promo.getDiscountValue());
                    if (result.getAppliedTrialDays() == null) result.setAppliedTrialDays(promo.getTrialDays());
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("Failed to evaluate promotion {} via Drools", promo.getName(), e);
            }
        }

        return results;
    }
}
