package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentProviderAdapter {
    PaymentCreationResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest);
    PaymentVerificationResponse verifyPaymentCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest);
    Object queryPaymentFromProvider(String paymentId, HttpServletRequest httpServletRequest);
    RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest);
    PaymentProvider getPaymentProviderName();
}