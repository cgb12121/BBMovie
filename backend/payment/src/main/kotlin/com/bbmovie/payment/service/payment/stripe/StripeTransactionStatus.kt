package com.bbmovie.payment.service.payment.stripe

import com.bbmovie.payment.entity.PaymentStatus
import com.bbmovie.payment.exception.StripePaymentException

enum class StripeTransactionStatus(
    val status: String,
    val paymentStatus: PaymentStatus,
    val description: String
) {
    REQUIRES_PAYMENT_METHOD("requires_payment_method", PaymentStatus.PENDING, "No payment method attached"),
    REQUIRES_CONFIRMATION("requires_confirmation", PaymentStatus.PENDING, "Awaiting confirmation"),
    REQUIRES_ACTION("requires_action", PaymentStatus.PENDING, "3D Secure or other action required"),
    PROCESSING("processing", PaymentStatus.PENDING, "Processing payment"),
    REQUIRES_CAPTURE("requires_capture", PaymentStatus.PENDING, "Authorized, awaiting capture"),
    CANCELED("canceled", PaymentStatus.CANCELLED, "Cancelled"),
    SUCCEEDED("succeeded", PaymentStatus.SUCCEEDED, "Success");

    companion object {
        fun fromStatus(status: String?): StripeTransactionStatus {
            return StripeTransactionStatus.entries.find { it.status.equals(status, ignoreCase = true) }
                ?: throw StripePaymentException("Unknown Stripe status: $status")
        }
    }
}
