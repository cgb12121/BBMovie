package com.example.bbmovie.service.payment.strategy;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface PaymentStrategy {
    PaymentProviderType getProviderType();
    PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpServletRequest);
}
