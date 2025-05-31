package com.example.bbmovie.service.payment.paypal;

import lombok.Getter;

@Getter
public enum PaypalTransactionStatus {
    APPROVED("APPROVED"),
    COMPLETED("COMPLETED") ;
    private final String status;


    PaypalTransactionStatus( String status ) {
        this.status = status;
    }
}
