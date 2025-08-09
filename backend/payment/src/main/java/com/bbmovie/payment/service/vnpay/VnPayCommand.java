package com.bbmovie.payment.service.vnpay;

import lombok.Getter;

@Getter
public enum VnPayCommand {
    QUERY_DR("querydr"),
    REFUND("refund"),
    PAY("pay");

    private final String command;

    VnPayCommand(String command) {
        this.command = command;
    }
}