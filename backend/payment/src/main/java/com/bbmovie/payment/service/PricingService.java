package com.bbmovie.payment.service;

import com.bbmovie.payment.entity.DiscountCampaign;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.Voucher;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.entity.enums.VoucherType;
import com.bbmovie.payment.repository.DiscountCampaignRepository;
import com.bbmovie.payment.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PricingService {

    private final DiscountCampaignRepository discountCampaignRepository;
    private final VoucherRepository voucherRepository;
    private final CurrencyConversionService currencyConversionService;

    @Autowired
    public PricingService(
            DiscountCampaignRepository discountCampaignRepository,
            VoucherRepository voucherRepository,
            CurrencyConversionService currencyConversionService
    ) {
        this.discountCampaignRepository = discountCampaignRepository;
        this.voucherRepository = voucherRepository;
        this.currencyConversionService = currencyConversionService;
    }

    @SuppressWarnings("java:S3776")
    public BigDecimal calculateFinalBasePrice(SubscriptionPlan plan, BillingCycle cycle, String userId, String voucherCode) {
        // 1) base on plan monthly price and cycle
        BigDecimal baseMonthly = plan.getMonthlyPrice();
        BigDecimal price;
        if (cycle == BillingCycle.ANNUAL) {
            BigDecimal annualBase = baseMonthly.multiply(BigDecimal.valueOf(12));
            BigDecimal annualDiscount = annualBase.multiply(plan.getAnnualDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = annualBase.subtract(annualDiscount);
        } else {
            price = baseMonthly;
        }

        // 2) apply an active campaign for this plan (take the max discount percent if multiple overlap)
        List<DiscountCampaign> actives = discountCampaignRepository
                .findByPlanAndActiveIsTrueAndStartAtBeforeAndEndAtAfter(plan, LocalDateTime.now(), LocalDateTime.now());
        Optional<DiscountCampaign> maxCampaign = actives.stream()
                .max(Comparator.comparing(DiscountCampaign::getDiscountPercent));
        if (maxCampaign.isPresent()) {
            BigDecimal campaignDiscount = price.multiply(maxCampaign.get().getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = price.subtract(campaignDiscount);
        }

        // 3) apply voucher if provided and valid (one usage per account is enforced elsewhere)
        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(voucherCode);
            if (opt.isPresent()) {
                Voucher v = opt.get();
                if (v.isActive()) {
                    boolean timeOk = v.isPermanent();
                    if (!timeOk) {
                        LocalDateTime now = LocalDateTime.now();
                        timeOk = (v.getStartAt() == null || !now.isBefore(v.getStartAt()))
                                && (v.getEndAt() == null || !now.isAfter(v.getEndAt()));
                    }
                    boolean userOk = v.getUserSpecificId() == null || (v.getUserSpecificId().equals(userId));
                    if (timeOk && userOk) {
                        if (v.getType() == VoucherType.PERCENTAGE && v.getPercentage() != null) {
                            BigDecimal voucherDiscount = price.multiply(v.getPercentage())
                                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            price = price.subtract(voucherDiscount);
                        } else if (v.getType() == VoucherType.FIXED_AMOUNT && v.getAmount() != null) {
                            price = price.subtract(v.getAmount());
                        }
                    }
                }
            }
        }

        // 4) clamp to zero
        return price.max(BigDecimal.ZERO);
    }
}


