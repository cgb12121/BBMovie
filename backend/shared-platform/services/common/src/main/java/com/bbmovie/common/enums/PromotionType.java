package com.bbmovie.common.enums;

public enum PromotionType {
    PERCENTAGE("PERCENTAGE"),
    FIXED_AMOUNT("FIXED_AMOUNT"),
    TRIAL_EXTENSION("TRIAL_EXTENSION");

    private final String value;

    PromotionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
