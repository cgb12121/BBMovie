package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.exception.TransactionNotFoundException;
import com.bbmovie.payment.exception.UnsupportedProviderException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

import static com.bbmovie.payment.entity.enums.PaymentProvider.*;

@Service
public class PaymentService {
    private final Map<String, PaymentProviderAdapter> providers;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentService(
            Map<String, PaymentProviderAdapter> providers,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.providers = providers;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public PaymentCreationResponse processPayment(
            String providerName, PaymentRequest request, HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        return provider.processPayment(request, httpServletRequest);
    }

    public PaymentVerificationResponse verifyPayment(
            String providerName, Map<String, String> paymentDataParams,
            HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        return provider.verifyPaymentCallback(paymentDataParams, httpServletRequest);
    }

    public RefundResponse refundPayment(String providerName, String paymentId, HttpServletRequest request) {
        PaymentProviderAdapter provider = providers.get(providerName);
        return provider.refundPayment(paymentId, request);
    }

    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new TransactionNotFoundException("Payment not found"));
        String paymentProvider;
        switch (txn.getPaymentProvider()) {
            case VNPAY -> paymentProvider = VNPAY.getName();
            case MOMO -> paymentProvider = MOMO.getName();
            case ZALOPAY -> paymentProvider = ZALOPAY.getName();
            case STRIPE -> paymentProvider = STRIPE.getName();
            case PAYPAL -> paymentProvider = PAYPAL.getName();
            default -> throw new UnsupportedProviderException("Payment provider not supported");
        }
        PaymentProviderAdapter provider = providers.get(paymentProvider);
        return provider.queryPaymentFromProvider(paymentId, httpServletRequest);
    }
}