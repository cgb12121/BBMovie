package com.bbmovie.common.enums;

public enum VerificationOutcome {
    AUTO_APPROVE("AUTO_APPROVE"),
    AUTO_REJECT("AUTO_REJECT"),
    NEEDS_REVIEW("NEEDS_REVIEW"),
    VERIFIED("VERIFIED"),
    REJECTED("REJECTED");

    private final String value;

    VerificationOutcome(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
