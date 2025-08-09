package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentProviderAdapter {
    PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest);
    PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest);
    RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest);

    PaymentProvider getPaymentProviderName();
}