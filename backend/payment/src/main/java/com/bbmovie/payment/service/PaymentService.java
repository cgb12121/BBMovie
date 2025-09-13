package com.bbmovie.payment.service;

import com.bbmovie.payment.config.payment.PaymentProviderProperties;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.exception.PaymentNotAvailableException;
import com.bbmovie.payment.exception.TransactionNotFoundException;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.utils.SimpleJwtDecoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.bbmovie.payment.entity.enums.PaymentProvider.*;

@Log4j2
@Service
public class PaymentService {

    private final Map<String, PaymentProviderAdapter> providers;
    private final PaymentProviderProperties properties;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public PaymentService(
            Map<String, PaymentProviderAdapter> providers,
            PaymentProviderProperties properties,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.providers = providers;
        this.properties = properties;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public PaymentCreationResponse createPayment(String jwtToken, SubscriptionPaymentRequest request, HttpServletRequest hsr) {
        PaymentProviderProperties.ProviderConfig toggle = properties.getProviders().get(request.provider());
        if (toggle == null || !toggle.isEnabled()) {
            throw new PaymentNotAvailableException(request.provider() + " is disabled: " + (toggle != null ? toggle.getReason() : "Unknown reason"));
        }
        String userId = SimpleJwtDecoder.getUserId(jwtToken);

        PaymentProviderAdapter adapter = providers.get(request.provider().toLowerCase());
        return adapter.createPaymentRequest(userId, request, hsr);
    }

    public PaymentVerificationResponse handleCallback(String provider, Map<String, String> params, HttpServletRequest hsr) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.handleCallback(params, hsr);
    }

    public PaymentVerificationResponse handleIpn(String provider, CallbackRequestContext ctx) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.handleIpn(ctx);
    }

    public PaymentVerificationResponse handleWebhook(String provider, CallbackRequestContext ctx) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.handleWebhook(ctx);
    }

    public RefundResponse refundPayment(String jwtToken, String paymentId, HttpServletRequest hsr) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(TransactionNotFoundException::new);

        String userId = SimpleJwtDecoder.getUserId(jwtToken);

        PaymentProviderAdapter adapter = providers.get(txn.getPaymentProvider().toString().toLowerCase());
        return adapter.refundPayment(userId, paymentId, hsr);
    }

    public Object queryPayment(String jwtToken, String paymentId) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(TransactionNotFoundException::new);

        String userId = SimpleJwtDecoder.getUserId(jwtToken);

        PaymentProviderAdapter provider = providers.get(txn.getPaymentProvider().toString().toLowerCase());
        return provider.queryPayment(userId, paymentId);
    }

    // Auto-cancel unpaid orders that passed expiration time. Runs every 5 minutes.
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void autoCancelExpiredUnpaid() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> expired = paymentTransactionRepository
                .findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, now);
        for (PaymentTransaction txn : expired) {
            if (txn.getStatus() == PaymentStatus.PENDING) {
                txn.setStatus(PaymentStatus.CANCELLED);
                txn.setCancelDate(now);
                txn.setStatus(PaymentStatus.AUTO_CANCELLED);
            }
        }
        if (!expired.isEmpty()) {
            paymentTransactionRepository.saveAll(expired);
        }
    }

    // End-of-day reconciliation. Runs at 23:55 server time daily.
    @Scheduled(cron = "0 55 23 * * *")
    public void reconcileDaily() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<PaymentTransaction> toCheck = paymentTransactionRepository
                .findByStatusAndTransactionDateAfter(PaymentStatus.PENDING, since);
        for (PaymentTransaction txn : toCheck) {
            String providerKey = switch (txn.getPaymentProvider()) {
                case VNPAY -> VNPAY.getName();
                case MOMO -> MOMO.getName();
                case ZALOPAY -> ZALOPAY.getName();
                case STRIPE -> STRIPE.getName();
                case PAYPAL -> PAYPAL.getName();
            };
            PaymentProviderAdapter adapter = providers.get(providerKey);
            try {
                adapter.queryPayment("SYSTEM" ,txn.getProviderTransactionId());
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}