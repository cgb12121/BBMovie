package com.bbmovie.payment.service;

import com.bbmovie.payment.config.payment.PaymentProviderProperties;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.exception.TransactionNotFoundException;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.UnsupportedProviderException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
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

    public PaymentCreationResponse createPayment(String provider, PaymentRequest request, HttpServletRequest hsr) {
        PaymentProviderProperties.ProviderConfig toggle = properties.getProviders().get(provider);
        if (toggle == null || !toggle.isEnabled()) {
            throw new IllegalStateException(provider + " is disabled: " + (toggle != null ? toggle.getReason() : "Unknown reason"));
        }
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.createPaymentRequest(request, hsr);
    }

    public PaymentVerificationResponse handleCallback(String provider, Map<String, String> params, HttpServletRequest hsr) {
        PaymentProviderAdapter adapter = providers.get(provider);
        PaymentVerificationResponse resp = adapter.handleCallback(params, hsr);
        if (resp != null && resp.isValid() && resp.getTransactionId() != null) {
            paymentTransactionRepository.findByPaymentGatewayId(resp.getTransactionId())
                    .ifPresent(txn -> scanAndFlagRapidSuccess(txn.getUserId()));
        }
        return resp;
    }

    public PaymentVerificationResponse handleIpn(String provider, CallbackRequestContext ctx) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.handleIpn(ctx);
    }

    public PaymentVerificationResponse handleWebhook(String provider, CallbackRequestContext ctx) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.handleWebhook(ctx);
    }

    public RefundResponse refundPayment(String provider, String paymentId, HttpServletRequest hsr) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.refundPayment(paymentId, hsr);
    }

    public RefundResponse refundPayment(String provider, String paymentId, java.math.BigDecimal amount, String reason, HttpServletRequest hsr) {
        PaymentProviderAdapter adapter = providers.get(provider);
        return adapter.refundPayment(paymentId, amount, reason, hsr);
    }

    public Object queryPayment(String paymentId, HttpServletRequest hsr) {
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
        return provider.queryPayment(paymentId, hsr);
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
                txn.setProviderStatus("EXPIRED_AUTO_CANCEL");
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
                adapter.queryPayment(txn.getPaymentGatewayId(), null);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    // Simple fraud heuristic: if multiple SUCCEEDED payments by the same user in a short window, flag a newer one.
    public void scanAndFlagRapidSuccess(String userId) {
        List<PaymentTransaction> recent = paymentTransactionRepository
                .findTop100ByUserIdAndStatusOrderByTransactionDateDesc(userId, PaymentStatus.SUCCEEDED);
        if (recent.size() < 2) return;
        PaymentTransaction latest = recent.getFirst();
        for (int i = 1; i < recent.size(); i++) {
            PaymentTransaction other = recent.get(i);
            if (latest.getTransactionDate().minusMinutes(5).isBefore(other.getTransactionDate())) {
                latest.setFraudFlag(true);
                latest.setFraudReason("Multiple successful payments by same user within 5 minutes");
                paymentTransactionRepository.save(latest);
                break;
            }
        }
    }
}