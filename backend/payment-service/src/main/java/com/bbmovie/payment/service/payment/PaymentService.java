package com.bbmovie.payment.service.payment;

import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentService {
    PaymentCreationResponse createPayment(String jwtToken, SubscriptionPaymentRequest request, HttpServletRequest hsr);

    PaymentVerificationResponse handleCallback(String provider, Map<String, String> params, HttpServletRequest hsr);

    PaymentVerificationResponse handleIpn(String provider, CallbackRequestContext ctx);

    PaymentVerificationResponse handleWebhook(String provider, CallbackRequestContext ctx);

    RefundResponse refundPayment(String jwtToken, String paymentId, HttpServletRequest hsr);

    Object queryPayment(String jwtToken, String paymentId);
}