package com.bbmovie.droolsengine.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class PromotionEvaluationContext {
    private UUID userId;
    private String userRole;
    private String userRegion;
    private Double cartValue;
    private String currency;
    private LocalDateTime currentDateTime;
    private String promotionId;
    private String droolsRuleId;
    private boolean eligible;
    private Double appliedDiscountValue;
    private Integer appliedTrialDays;
    private String reason;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUserRegion() {
        return userRegion;
    }

    public void setUserRegion(String userRegion) {
        this.userRegion = userRegion;
    }

    public Double getCartValue() {
        return cartValue;
    }

    public void setCartValue(Double cartValue) {
        this.cartValue = cartValue;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getCurrentDateTime() {
        return currentDateTime;
    }

    public void setCurrentDateTime(LocalDateTime currentDateTime) {
        this.currentDateTime = currentDateTime;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getDroolsRuleId() {
        return droolsRuleId;
    }

    public void setDroolsRuleId(String droolsRuleId) {
        this.droolsRuleId = droolsRuleId;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public Double getAppliedDiscountValue() {
        return appliedDiscountValue;
    }

    public void setAppliedDiscountValue(Double appliedDiscountValue) {
        this.appliedDiscountValue = appliedDiscountValue;
    }

    public Integer getAppliedTrialDays() {
        return appliedTrialDays;
    }

    public void setAppliedTrialDays(Integer appliedTrialDays) {
        this.appliedTrialDays = appliedTrialDays;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
