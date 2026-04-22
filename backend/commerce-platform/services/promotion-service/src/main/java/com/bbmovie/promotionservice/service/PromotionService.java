package com.bbmovie.promotionservice.service;

import com.bbmovie.promotionservice.dto.CouponApplyRequest;
import com.bbmovie.promotionservice.dto.PromotionEvaluationContext;
import com.bbmovie.promotionservice.entity.Coupon;
import com.bbmovie.promotionservice.entity.Promotion;
import com.bbmovie.promotionservice.entity.UserPromotionUsage;
import com.bbmovie.promotionservice.enums.PromotionStatus;
import com.bbmovie.promotionservice.rules.PromotionRule;
import com.bbmovie.promotionservice.rules.PromotionRuleLoader;
import com.bbmovie.promotionservice.repository.CouponRepository;
import com.bbmovie.promotionservice.repository.PromotionRepository;
import com.bbmovie.promotionservice.repository.UserPromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;
    private final PromotionRuleLoader ruleLoader;
    private final Clock clock;

    @Transactional
    public PromotionEvaluationContext applyCoupon(CouponApplyRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
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
            return PromotionEvaluationContext.builder()
                .eligible(false)
                .reason("Coupon is not active")
                .build();
        }

        if (promotion.getStartDate().isAfter(now)) {
            return PromotionEvaluationContext.builder()
                .eligible(false)
                .reason("Coupon is not yet valid")
                .build();
        }

        if (promotion.getEndDate().isBefore(now)) {
            return PromotionEvaluationContext.builder()
                .eligible(false)
                .reason("Coupon has expired")
                .build();
        }

        if (request.getCartValue() != null && coupon.getMinPurchaseAmount() != null && request.getCartValue() < coupon.getMinPurchaseAmount()) {
            return PromotionEvaluationContext.builder()
                .eligible(false)
                .reason("Minimum purchase amount not met")
                .build();
        }

        if (userPromotionUsageRepository.existsByUserIdAndPromotionId(request.getUserId(), promotion.getId())) {
            return PromotionEvaluationContext.builder()
                .eligible(false)
                .reason("User has already used this promotion")
                .build();
        }

        // Apply
        int updatedRows = couponRepository.incrementUsage(coupon.getId());
        if (updatedRows == 0) {
            throw new IllegalStateException("Coupon usage limit reached or coupon not found");
        }

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
        LocalDateTime now = LocalDateTime.now(clock);
        List<PromotionEvaluationContext> results = new ArrayList<>();
        for (PromotionRule rule : ruleLoader.currentRules()) {
            if (!matchesRule(rule, context, now)) {
                continue;
            }
            Optional<Promotion> promoOpt = findActivePromotion(rule.getPromotionId());
            if (promoOpt.isEmpty()) {
                continue;
            }
            Promotion promo = promoOpt.get();
            PromotionEvaluationContext match = PromotionEvaluationContext.builder()
                    .userId(context.getUserId())
                    .userRole(context.getUserRole())
                    .userRegion(context.getUserRegion())
                    .cartValue(context.getCartValue())
                    .currency(context.getCurrency())
                    .promotionId(promo.getId().toString())
                    .eligible(true)
                    .appliedDiscountValue(rule.getDiscountValue() != null ? rule.getDiscountValue() : promo.getDiscountValue())
                    .appliedTrialDays(rule.getTrialDays() != null ? rule.getTrialDays() : promo.getTrialDays())
                    .reason("Rule matched: " + rule.getName())
                    .build();

            results.add(match);
        }

        return results;
    }

    private boolean matchesRule(PromotionRule rule, PromotionEvaluationContext context, LocalDateTime now) {
        LocalDateTime evaluationTime = context.getCurrentDateTime() != null ? context.getCurrentDateTime() : now;

        if (rule.getStartDate() != null && evaluationTime.isBefore(rule.getStartDate())) {
            return false;
        }

        if (rule.getEndDate() != null && evaluationTime.isAfter(rule.getEndDate())) {
            return false;
        }

        if (rule.getMinCartValue() != null && context.getCartValue() != null && context.getCartValue() < rule.getMinCartValue()) {
            return false;
        }

        if (rule.getCurrency() != null && context.getCurrency() != null && !rule.getCurrency().equalsIgnoreCase(context.getCurrency())) {
            return false;
        }

        if (rule.getUserRole() != null && context.getUserRole() != null && !rule.getUserRole().equalsIgnoreCase(context.getUserRole())) {
            return false;
        }

        if (rule.getUserRegion() != null && context.getUserRegion() != null && !rule.getUserRegion().equalsIgnoreCase(context.getUserRegion())) {
            return false;
        }

        if (rule.isOneTimePerUser() && context.getUserId() != null && rule.getPromotionId() != null) {
            if (userPromotionUsageRepository.existsByUserIdAndPromotionId(context.getUserId(), rule.getPromotionId())) {
                return false;
            }
        }
        return true;
    }

    private Optional<Promotion> findActivePromotion(UUID promotionId) {
        if (promotionId == null) {
            return Optional.empty();
        }
        return promotionRepository.findById(promotionId)
                .filter(p -> p.getStatus() == PromotionStatus.ACTIVE);
    }
}
