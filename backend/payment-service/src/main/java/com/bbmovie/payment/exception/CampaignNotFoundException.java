package com.bbmovie.payment.exception;

public class CampaignNotFoundException extends RuntimeException {
    public CampaignNotFoundException() {
        super("Campaign not found");
    }
}
