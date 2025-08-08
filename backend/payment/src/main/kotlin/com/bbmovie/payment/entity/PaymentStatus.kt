package com.bbmovie.payment.entity

enum class PaymentStatus(val status: String) {
    PENDING("PENDING"),
    SUCCEEDED("SUCCEEDED"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    REFUNDED("REFUNDED");

    val getStatus: String get() = this.status
}