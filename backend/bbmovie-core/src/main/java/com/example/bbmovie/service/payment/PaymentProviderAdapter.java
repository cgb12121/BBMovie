package com.example.bbmovie.service.payment;

import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentProviderAdapter {
    PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest);
    PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest);
    RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest);
}