package com.bbmovie.common.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public class PromotionEvaluationContext {
    private UUID userId;
    private String userRole;
    private String userRegion;
    private Double cartValue;
    private String currency;
    private LocalDateTime currentDateTime;
    private String promotionId; // ID of the promotion being evaluated
    private String droolsRuleId;

    // Output from Drools
    private boolean eligible;
    private Double appliedDiscountValue;
    private Integer appliedTrialDays;
    private String reason;

    public PromotionEvaluationContext() {
    }

    public PromotionEvaluationContext(
            UUID userId,
            String userRole,
            String userRegion,
            Double cartValue,
            String currency,
            LocalDateTime currentDateTime,
            String promotionId,
            String droolsRuleId,
            boolean eligible,
            Double appliedDiscountValue,
            Integer appliedTrialDays,
            String reason
    ) {
        this.userId = userId;
        this.userRole = userRole;
        this.userRegion = userRegion;
        this.cartValue = cartValue;
        this.currency = currency;
        this.currentDateTime = currentDateTime;
        this.promotionId = promotionId;
        this.droolsRuleId = droolsRuleId;
        this.eligible = eligible;
        this.appliedDiscountValue = appliedDiscountValue;
        this.appliedTrialDays = appliedTrialDays;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public static final class Builder {
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

        private Builder() {
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userRole(String userRole) {
            this.userRole = userRole;
            return this;
        }

        public Builder userRegion(String userRegion) {
            this.userRegion = userRegion;
            return this;
        }

        public Builder cartValue(Double cartValue) {
            this.cartValue = cartValue;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder currentDateTime(LocalDateTime currentDateTime) {
            this.currentDateTime = currentDateTime;
            return this;
        }

        public Builder promotionId(String promotionId) {
            this.promotionId = promotionId;
            return this;
        }

        public Builder droolsRuleId(String droolsRuleId) {
            this.droolsRuleId = droolsRuleId;
            return this;
        }

        public Builder eligible(boolean eligible) {
            this.eligible = eligible;
            return this;
        }

        public Builder appliedDiscountValue(Double appliedDiscountValue) {
            this.appliedDiscountValue = appliedDiscountValue;
            return this;
        }

        public Builder appliedTrialDays(Integer appliedTrialDays) {
            this.appliedTrialDays = appliedTrialDays;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public PromotionEvaluationContext build() {
            return new PromotionEvaluationContext(
                    userId,
                    userRole,
                    userRegion,
                    cartValue,
                    currency,
                    currentDateTime,
                    promotionId,
                    droolsRuleId,
                    eligible,
                    appliedDiscountValue,
                    appliedTrialDays,
                    reason
            );
        }
    }
}
