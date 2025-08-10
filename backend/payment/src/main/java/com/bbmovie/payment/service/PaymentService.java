package com.bbmovie.payment.service;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

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

    public PaymentResponse processPayment(
            String providerName, PaymentRequest request, HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider " + providerName + " not supported");
        }
        return provider.processPayment(request, httpServletRequest);
    }

    public PaymentVerification verifyPayment(
            String providerName, Map<String, String> paymentDataParams,
            HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider " + providerName + " not supported");
        }

        PaymentVerification verification = provider.verifyPayment(paymentDataParams, httpServletRequest);
        if (verification.isValid()) {
            paymentTransactionRepository.findByPaymentGatewayId(verification.getTransactionId())
                    .ifPresent(entity -> {
                        entity.setStatus(PaymentStatus.SUCCEEDED);
                        entity.setLastModifiedDate(LocalDateTime.now());
                        paymentTransactionRepository.save(entity);
                    });
        }
        return verification;
    }

    public RefundResponse refundPayment(String providerName, String paymentId, HttpServletRequest request) {
        PaymentProviderAdapter provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider " + providerName + " not supported");
        }

        RefundResponse response = provider.refundPayment(paymentId, request);
        if ("SUCCESS".equals(response.getStatus())) {
            paymentTransactionRepository.findById(UUID.fromString(paymentId))
                    .ifPresent(entity -> {
                        entity.setStatus(PaymentStatus.REFUNDED);
                        entity.setLastModifiedDate(LocalDateTime.now());
                        paymentTransactionRepository.save(entity);
                    });
        }
        return response;
    }

    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        String paymentProvider;
        switch (txn.getPaymentProvider()) {
            case VNPAY -> paymentProvider = "vnpayProvider";
            case MOMO -> paymentProvider = "momoAdapter";
            case ZALO_PAY -> paymentProvider = "zalopayAdapter";
            case STRIPE -> paymentProvider = "stripeAdapter";
            case PAYPAL -> paymentProvider = "paypalAdapter";
            default -> throw new IllegalArgumentException("Payment provider not supported");
        }
        PaymentProviderAdapter provider = providers.get(paymentProvider);
        return provider.queryPayment(paymentId, httpServletRequest);
    }
}