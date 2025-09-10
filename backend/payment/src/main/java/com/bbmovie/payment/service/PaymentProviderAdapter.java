package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentProviderAdapter {

    PaymentCreationResponse createPaymentRequest(String userId, SubscriptionPaymentRequest request, HttpServletRequest hsr);

    Object queryPayment(String userId, String paymentId);

    RefundResponse refundPayment(String userId, String paymentId, HttpServletRequest hsr);

    PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest hsr);

    /**
     * Optional: IPN-like callbacks (form/query params). Default bridges to legacy handleCallback.
     */
    default PaymentVerificationResponse handleIpn(CallbackRequestContext ctx) {
        Map<String, String> params = (ctx.getFormParams() != null && !ctx.getFormParams().isEmpty())
                ? ctx.getFormParams()
                : ctx.getQueryParams();
        return this.handleCallback(params, ctx.getHttpServletRequest());
    }

    /**
     * Optional: Webhook callbacks (raw body and headers). Default unsupported.
     */
    default PaymentVerificationResponse handleWebhook(CallbackRequestContext ctx) {
        throw new UnsupportedOperationException("Webhook not supported by this provider");
    }
}