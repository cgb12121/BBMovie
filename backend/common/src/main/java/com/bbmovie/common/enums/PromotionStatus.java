package com.bbmovie.common.enums;

public enum PromotionStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    EXPIRED("EXPIRED"),
    SCHEDULED("SCHEDULED");

    private final String value;

    PromotionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
