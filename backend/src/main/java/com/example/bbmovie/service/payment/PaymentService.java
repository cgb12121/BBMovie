package com.example.bbmovie.service.payment;

import com.example.bbmovie.entity.PaymentTransaction;
import com.example.bbmovie.entity.enumerate.PaymentStatus;
import com.example.bbmovie.repository.PaymentTransactionRepository;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

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
        if (verification.isValid()) {
            paymentTransactionRepository.findById(Long.valueOf(verification.getTransactionId()))
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
            paymentTransactionRepository.findById(Long.valueOf(paymentId))
                    .ifPresent(entity -> {
                        entity.setStatus(PaymentStatus.REFUNDED);
                        entity.setLastModifiedDate(LocalDateTime.now());
                        paymentTransactionRepository.save(entity);
                    });
        }
        return response;
    }
}