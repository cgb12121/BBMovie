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

    public PaymentProviderAdapter getProvider(String name) {
        return providers.get(name);
    }

    public PaymentResponse processPayment(
            String providerName,
            PaymentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider " + providerName + " not supported");
        }

        PaymentResponse response = provider.processPayment(request, httpServletRequest);
        PaymentTransaction entity = PaymentTransaction.builder()
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentProvider(provider.getPaymentProviderName())
                .paymentMethod(request.getPaymentMethodId())
                .status(PaymentStatus.PENDING)
                .cancelDate(null)
                .paymentGatewayId(response.getTransactionId())
                .userId(request.getUserId())
                .transactionDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethodId())
                .providerStatus(String.valueOf(response.getStatus()))
                .build();

        paymentTransactionRepository.save(entity);
        return response;
    }

    public PaymentVerification verifyPayment(
            String providerName,
            Map<String, String> paymentData,
            HttpServletRequest httpServletRequest
    ) {
        PaymentProviderAdapter provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Provider " + providerName + " not supported");
        }

        PaymentVerification verification = provider.verifyPayment(paymentData, httpServletRequest);
        if (verification.isSuccess()) {
            paymentTransactionRepository.findById(UUID.fromString(verification.getTransactionId()))
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
}