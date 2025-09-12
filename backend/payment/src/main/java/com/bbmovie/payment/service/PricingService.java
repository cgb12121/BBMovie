package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.DiscountResult;
import com.bbmovie.payment.dto.PricingBreakdown;
import com.bbmovie.payment.dto.request.TaxRateRequest;
import com.bbmovie.payment.entity.DiscountCampaign;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.Voucher;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.entity.enums.VoucherType;
import com.bbmovie.payment.repository.DiscountCampaignRepository;
import com.bbmovie.payment.repository.VoucherRepository;
import com.bbmovie.payment.service.tax.TaxService;
import org.javamoney.moneta.Money;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PricingService {

    private final DiscountCampaignRepository discountCampaignRepository;
    private final VoucherRepository voucherRepository;
    private final CurrencyConversionService currencyConversionService;
    private final TaxService taxService;

    @Autowired
    public PricingService(
            DiscountCampaignRepository discountCampaignRepository,
            VoucherRepository voucherRepository,
            CurrencyConversionService currencyConversionService,
            TaxService taxService
    ) {
        this.discountCampaignRepository = discountCampaignRepository;
        this.voucherRepository = voucherRepository;
        this.currencyConversionService = currencyConversionService;
        this.taxService = taxService;
    }

    public PricingBreakdown calculate(
            SubscriptionPlan plan,
            BillingCycle cycle,
            CurrencyUnit currency,
            String userId,
            String ipAddress,
            String voucherCode
    ) {
        BigDecimal baseMonthly = plan.getMonthlyPrice();

        DiscountResult cycleDiscount = calculateCycleDiscount(plan, cycle);
        DiscountResult campaignDiscount = calculateCampaignDiscount(plan, cycleDiscount.amount());
        DiscountResult voucherDiscount = calculateVoucherDiscount(voucherCode, userId, campaignDiscount.amount());
        BigDecimal subtotal = campaignDiscount.amount()
                .subtract(voucherDiscount.amount())
                .max(BigDecimal.ZERO);

        DiscountResult tax = calculateTax(ipAddress, subtotal);

        BigDecimal finalPrice = subtotal.add(tax.amount());

        if (!currency.equals(plan.getBaseCurrency())) {
            return convertBreakdown(
                    plan.getBaseCurrency(), currency,
                    baseMonthly, cycleDiscount, campaignDiscount, voucherDiscount,
                    tax, finalPrice
            );
        }

        return new PricingBreakdown(
                baseMonthly,
                Map.of(cycleDiscount.rate(), cycleDiscount.amount()),
                Map.of(campaignDiscount.rate(), campaignDiscount.amount()),
                Map.of(voucherDiscount.rate(), voucherDiscount.amount()),
                cycleDiscount.amount().add(campaignDiscount.amount()).add(voucherDiscount.amount()),
                Map.of(tax.rate(), tax.amount()),
                finalPrice
        );
    }

    private DiscountResult calculateCycleDiscount(SubscriptionPlan plan, BillingCycle cycle) {
        if (cycle == BillingCycle.ANNUAL) {
            BigDecimal annualBase = plan.getMonthlyPrice().multiply(BigDecimal.valueOf(12));
            BigDecimal discount = annualBase.multiply(plan.getAnnualDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            return new DiscountResult(plan.getAnnualDiscountPercent(), discount);
        }
        return DiscountResult.none();
    }

    private DiscountResult calculateCampaignDiscount(SubscriptionPlan plan, BigDecimal currentPrice) {
        List<DiscountCampaign> actives = discountCampaignRepository
                .findByPlanAndActiveIsTrueAndStartAtBeforeAndEndAtAfter(
                        plan, LocalDateTime.now(), LocalDateTime.now());

        return actives.stream()
                .max(Comparator.comparing(DiscountCampaign::getDiscountPercent))
                .map(c -> {
                    BigDecimal discount = currentPrice.multiply(c.getDiscountPercent())
                            .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                    return new DiscountResult(c.getDiscountPercent(), discount);
                })
                .orElse(DiscountResult.none());
    }

    private DiscountResult calculateVoucherDiscount(String voucherCode, String userId, BigDecimal currentPrice) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return DiscountResult.none();
        }

        return voucherRepository.findByCodeIgnoreCase(voucherCode)
                .filter(Voucher::isActive)
                .filter(v -> {
                    LocalDateTime now = LocalDateTime.now();
                    boolean timeOk = v.isPermanent() ||
                            (v.getStartAt() == null || !now.isBefore(v.getStartAt())) &&
                                    (v.getEndAt() == null || !now.isAfter(v.getEndAt()));
                    boolean userOk = v.getUserSpecificId() == null || v.getUserSpecificId().equals(userId);
                    return timeOk && userOk;
                })
                .map(v -> {
                    if (v.getType() == VoucherType.PERCENTAGE && v.getPercentage() != null) {
                        BigDecimal discount = currentPrice.multiply(v.getPercentage())
                                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                        return new DiscountResult(v.getPercentage(), discount);
                    } else if (v.getType() == VoucherType.FIXED_AMOUNT && v.getAmount() != null) {
                        return new DiscountResult(BigDecimal.ZERO, v.getAmount());
                    }
                    return DiscountResult.none();
                })
                .orElse(DiscountResult.none());
    }

    private DiscountResult calculateTax(String ipAddress, BigDecimal subtotal) {
        BigDecimal taxPercent = taxService.taxRate(
                TaxRateRequest.builder().ipAddress(ipAddress).build()
        );

        if (taxPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return DiscountResult.none();
        }

        BigDecimal taxAmount = subtotal.multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

        return new DiscountResult(taxPercent, taxAmount);
    }

    @SuppressWarnings("squid:S107")
    private PricingBreakdown convertBreakdown(
            CurrencyUnit baseCurrency,
            CurrencyUnit targetCurrency,
            BigDecimal baseMonthly,
            DiscountResult cycle,
            DiscountResult campaign,
            DiscountResult voucher,
            DiscountResult tax,
            BigDecimal finalPrice
    ) {
        BigDecimal baseMonthlyConv = convert(baseMonthly, baseCurrency, targetCurrency);
        DiscountResult cycleConv = new DiscountResult(cycle.rate(), convert(cycle.amount(), baseCurrency, targetCurrency));
        DiscountResult campaignConv = new DiscountResult(campaign.rate(), convert(campaign.amount(), baseCurrency, targetCurrency));
        DiscountResult voucherConv = new DiscountResult(voucher.rate(), convert(voucher.amount(), baseCurrency, targetCurrency));
        DiscountResult taxConv = new DiscountResult(tax.rate(), convert(tax.amount(), baseCurrency, targetCurrency));
        BigDecimal finalPriceConv = convert(finalPrice, baseCurrency, targetCurrency);

        BigDecimal totalDiscountConv = cycleConv.amount().add(campaignConv.amount()).add(voucherConv.amount());

        return new PricingBreakdown(
                baseMonthlyConv,
                Map.of(cycleConv.rate(), cycleConv.amount()),
                Map.of(campaignConv.rate(), campaignConv.amount()),
                Map.of(voucherConv.rate(), voucherConv.amount()),
                totalDiscountConv,
                Map.of(taxConv.rate(), taxConv.amount()),
                finalPriceConv
        );
    }

    private BigDecimal convert(BigDecimal amount, CurrencyUnit from, CurrencyUnit to) {
        return currencyConversionService.convert(Money.of(amount, from), to.getCurrencyCode())
                .getNumber().numberValue(BigDecimal.class);
    }

    @SuppressWarnings("all")
    @Deprecated(since = "1.0.0", forRemoval = true)
    public PricingBreakdown calculateBreakdown(
            SubscriptionPlan plan,
            BillingCycle cycle,
            CurrencyUnit currency,
            String userId,
            String ipAddress,
            String voucherCode
    ) {
        BigDecimal baseMonthly = plan.getMonthlyPrice();

        BigDecimal cycleDiscount = BigDecimal.ZERO;
        BigDecimal campaignDiscount = BigDecimal.ZERO;
        BigDecimal voucherDiscount = BigDecimal.ZERO;

        BigDecimal price;
        if (cycle == BillingCycle.ANNUAL) {
            BigDecimal annualBase = baseMonthly.multiply(BigDecimal.valueOf(12));
            cycleDiscount = annualBase.multiply(plan.getAnnualDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = annualBase.subtract(cycleDiscount);
        } else {
            price = baseMonthly;
        }

        // campaigns...
        List<DiscountCampaign> actives = discountCampaignRepository
                .findByPlanAndActiveIsTrueAndStartAtBeforeAndEndAtAfter(plan, LocalDateTime.now(), LocalDateTime.now());
        Optional<DiscountCampaign> maxCampaign = actives.stream()
                .max(Comparator.comparing(DiscountCampaign::getDiscountPercent));
        if (maxCampaign.isPresent()) {
            campaignDiscount = price.multiply(maxCampaign.get().getDiscountPercent())
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            price = price.subtract(campaignDiscount);
        }

        // voucher...
        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(voucherCode);
            if (opt.isPresent()) {
                Voucher v = opt.get();
                if (v.isActive()) {
                    LocalDateTime now = LocalDateTime.now();
                    boolean timeOk = v.isPermanent() || (v.getStartAt() == null || !now.isBefore(v.getStartAt()))
                            && (v.getEndAt() == null || !now.isAfter(v.getEndAt()));
                    boolean userOk = v.getUserSpecificId() == null || v.getUserSpecificId().equals(userId);
                    if (timeOk && userOk) {
                        if (v.getType() == VoucherType.PERCENTAGE && v.getPercentage() != null) {
                            voucherDiscount = price.multiply(v.getPercentage())
                                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            price = price.subtract(voucherDiscount);
                        } else if (v.getType() == VoucherType.FIXED_AMOUNT && v.getAmount() != null) {
                            voucherDiscount = v.getAmount();
                            price = price.subtract(voucherDiscount);
                        }
                    }
                }
            }
        }

        BigDecimal subtotal = price.max(BigDecimal.ZERO);

        BigDecimal taxPercent = taxService.taxRate(
                TaxRateRequest.builder()
                        .ipAddress(ipAddress)
                        .build()
        );
        BigDecimal taxAmount = BigDecimal.ZERO;
        if (taxPercent.compareTo(BigDecimal.ZERO) > 0) {
            taxAmount = subtotal.multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }

        BigDecimal finalPrice = subtotal.add(taxAmount);

        Map<BigDecimal, BigDecimal> cycleMap = Collections.singletonMap(
                cycle == BillingCycle.ANNUAL
                        ? plan.getAnnualDiscountPercent()
                        : BigDecimal.ZERO,
                cycleDiscount
        );
        Map<BigDecimal, BigDecimal> campaignMap = Collections.singletonMap(
                Optional.of(
                        maxCampaign.map(DiscountCampaign::getDiscountPercent)
                                .orElse(BigDecimal.ZERO)
                ).orElse(BigDecimal.ZERO),
                campaignDiscount
        );
        Map<BigDecimal, BigDecimal> voucherMap;
        if (voucherCode != null && !voucherCode.isBlank()) {
            Optional<Voucher> opt = voucherRepository.findByCodeIgnoreCase(voucherCode);
            if (opt.isPresent()) {
                Voucher v = opt.get();
                BigDecimal rate = v.getType() == VoucherType.PERCENTAGE
                        ? v.getPercentage()
                        : BigDecimal.ZERO;
                BigDecimal amount = v.getType() == VoucherType.FIXED_AMOUNT
                        ? v.getAmount()
                        : voucherDiscount;
                voucherMap = Collections.singletonMap(
                        rate != null
                                ? rate
                                : BigDecimal.ZERO,
                        amount != null
                                ? amount
                                : BigDecimal.ZERO);
            } else {
                voucherMap = Collections.emptyMap();
            }
        } else {
            voucherMap = Collections.emptyMap();
        }

        BigDecimal totalDiscount = cycleDiscount.add(campaignDiscount).add(voucherDiscount);

        if (!currency.equals(plan.getBaseCurrency())) {
            BigDecimal baseMonthlyConv = currencyConversionService.convert(
                    Money.of(baseMonthly, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal cycleDiscountConv = currencyConversionService.convert(
                    Money.of(cycleDiscount, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal campaignDiscountConv = currencyConversionService.convert(
                    Money.of(campaignDiscount, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal voucherDiscountConv = currencyConversionService.convert(
                    Money.of(voucherDiscount, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal taxAmountConv = currencyConversionService.convert(
                    Money.of(taxAmount, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal totalDiscountConv = currencyConversionService.convert(
                    Money.of(totalDiscount, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            BigDecimal finalPriceConv = currencyConversionService.convert(
                    Money.of(finalPrice, plan.getBaseCurrency()),
                    currency.getCurrencyCode()
            ).getNumber().numberValue(BigDecimal.class);

            Map<BigDecimal, BigDecimal> cycleMapConv = Collections.singletonMap(
                    cycle == BillingCycle.ANNUAL
                            ? plan.getAnnualDiscountPercent()
                            : BigDecimal.ZERO,
                    cycleDiscountConv
            );
            Map<BigDecimal, BigDecimal> campaignMapConv = Collections.singletonMap(
                    Optional.of(
                            maxCampaign.map(DiscountCampaign::getDiscountPercent)
                                    .orElse(BigDecimal.ZERO)
                    ).orElse(BigDecimal.ZERO),
                    campaignDiscountConv
            );
            Map<BigDecimal, BigDecimal> voucherMapConv;
            if (voucherCode != null && !voucherCode.isBlank()) {
                Optional<Voucher> optV = voucherRepository.findByCodeIgnoreCase(voucherCode);
                if (optV.isPresent()) {
                    Voucher v = optV.get();
                    BigDecimal rate = v.getType() == VoucherType.PERCENTAGE
                            ? v.getPercentage()
                            : BigDecimal.ZERO;
                    BigDecimal amount = v.getType() == VoucherType.FIXED_AMOUNT
                            ? voucherDiscountConv
                            : voucherDiscountConv.multiply(rate)
                                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                    voucherMapConv = Collections.singletonMap(rate != null ? rate : BigDecimal.ZERO, amount);
                } else {
                    voucherMapConv = Collections.emptyMap();
                }
            } else {
                voucherMapConv = Collections.emptyMap();
            }

            Map<BigDecimal, BigDecimal> taxMapConv = Collections.singletonMap(taxPercent, taxAmountConv);

            return new PricingBreakdown(
                    baseMonthlyConv,
                    cycleMapConv,
                    campaignMapConv,
                    voucherMapConv,
                    totalDiscountConv,
                    taxMapConv,
                    finalPriceConv
            );
        }

        Map<BigDecimal, BigDecimal> taxMap = Collections.singletonMap(taxPercent, taxAmount);

        return new PricingBreakdown(
                baseMonthly,
                cycleMap,
                campaignMap,
                voucherMap,
                totalDiscount,
                taxMap,
                finalPrice
        );
    }
}
