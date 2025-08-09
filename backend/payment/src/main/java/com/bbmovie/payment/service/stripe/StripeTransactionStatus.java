package com.bbmovie.payment.service.stripe;

import com.bbmovie.payment.exception.StripePaymentException;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import lombok.Getter;

@Getter
public enum StripeTransactionStatus {
    REQUIRES_PAYMENT_METHOD("requires_payment_method", PaymentStatus.PENDING, "No payment method attached"),
    REQUIRES_CONFIRMATION("requires_confirmation", PaymentStatus.PENDING, "Awaiting confirmation"),
    REQUIRES_ACTION("requires_action", PaymentStatus.PENDING, "3D Secure or other action required"),
    PROCESSING("processing", PaymentStatus.PENDING, "Processing payment"),
    REQUIRES_CAPTURE("requires_capture", PaymentStatus.PENDING, "Authorized, awaiting capture"),
    CANCELED("canceled", PaymentStatus.CANCELLED, "Cancelled"),
    SUCCEEDED("succeeded", PaymentStatus.SUCCEEDED, "Success");

    private final String status;
    private final PaymentStatus paymentStatus;
    private final String description;

    StripeTransactionStatus(String status, PaymentStatus paymentStatus, String description) {
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.description = description;
    }

    public static StripeTransactionStatus fromStatus(String status) {
        for (StripeTransactionStatus value : values()) {
            if (value.status.equalsIgnoreCase(status)) {
                return value;
            }
        }
        throw new StripePaymentException("Unknown Stripe status: " + status);
    }
}
