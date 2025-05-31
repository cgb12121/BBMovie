package com.example.bbmovie.service.payment.stripe;

import com.example.bbmovie.exception.StripePaymentException;
import com.example.bbmovie.service.payment.PaymentStatus;
import lombok.Getter;

@Getter
public enum StripeTransactionStatus {
    REQUIRES_PAYMENT_METHOD("requires_payment_method", PaymentStatus.PENDING, "No payment method attached"),
    REQUIRES_CONFIRMATION("requires_confirmation", PaymentStatus.PENDING, "Payment method attached, awaiting confirmation"),
    REQUIRES_ACTION("requires_action", PaymentStatus.PENDING, "Additional action required (e.g., 3D Secure)"),
    PROCESSING("processing", PaymentStatus.PENDING, "Payment is being processed"),
    REQUIRES_CAPTURE("requires_capture", PaymentStatus.PENDING, "Payment authorized, awaiting capture"),
    CANCELED("canceled", PaymentStatus.CANCELLED, "Payment was canceled"),
    SUCCEEDED("succeeded", PaymentStatus.SUCCEEDED, "Payment completed successfully");

    private final String status;
    private final PaymentStatus paymentStatus;
    private final String description;

    StripeTransactionStatus(String status, PaymentStatus paymentStatus, String description) {
        this.status = status;
        this.paymentStatus = paymentStatus;
        this.description = description;
    }

    public static StripeTransactionStatus fromStatus(String status) {
        if (status == null) {
            return null;
        }
        for (StripeTransactionStatus stripeStatus : values()) {
            if (stripeStatus.getStatus().equalsIgnoreCase(status)) {
                return stripeStatus;
            }
        }
        throw new StripePaymentException("Unknown Stripe status: " + status);
    }
}