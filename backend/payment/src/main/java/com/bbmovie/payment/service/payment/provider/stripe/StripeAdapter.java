package com.bbmovie.payment.service.payment.provider.stripe;

import com.bbmovie.payment.config.payment.StripeProperties;
import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.dto.PricingBreakdown;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.exception.PaymentCacheException;
import com.bbmovie.payment.service.SubscriptionPlanService;
import com.bbmovie.payment.service.cache.RedisService;
import com.bbmovie.payment.service.nats.PaymentEventProducer;
import com.bbmovie.payment.service.payment.PricingService;
import com.bbmovie.payment.service.PaymentRecordService;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.exception.StripePaymentException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.exception.TransactionNotFoundException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.i18n.PaymentI18nService;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service("stripe")
public class StripeAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PricingService pricingService;
    private final PaymentRecordService paymentRecordService;
    private final PaymentI18nService paymentI18nService;
    private final RedisService redisService;
    private final PaymentEventProducer paymentEventProducer;

    @Autowired
    public StripeAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentI18nService paymentI18nService,
            StripeProperties properties,
            SubscriptionPlanService subscriptionPlanService,
            PricingService pricingService,
            PaymentRecordService paymentRecordService,
            RedisService redisService,
            PaymentEventProducer paymentEventProducer
    ) {
        Stripe.apiKey = properties.getSecretKey();
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionPlanService = subscriptionPlanService;
        this.paymentI18nService = paymentI18nService;
        this.pricingService = pricingService;
        this.paymentRecordService = paymentRecordService;
        this.paymentEventProducer = paymentEventProducer;
        this.redisService = redisService;
    }

    @Override
    @Transactional(noRollbackFor = PaymentCacheException.class)
    public PaymentCreationResponse createPaymentRequest(String userId, SubscriptionPaymentRequest request, HttpServletRequest hsr
    ) {
        SubscriptionPlan plan = subscriptionPlanService.getById(UUID.fromString(request.subscriptionPlanId()));
        PricingBreakdown breakdown = pricingService.calculate(
                plan,
                request.billingCycle(),
                plan.getBaseCurrency(),
                userId,
                null,
                request.voucherCode()
        );
        BigDecimal amountInBaseCurrency = breakdown.finalPrice();

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountInBaseCurrency.multiply(BigDecimal.valueOf(100)).longValueExact());
            params.put("currency", plan.getBaseCurrency().getCurrencyCode());
            params.put("description", "Subscription");
            params.put("confirmation_method", "manual");
            params.put("confirm", true);

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(
                    userId, plan, amountInBaseCurrency, plan.getBaseCurrency(),
                    PaymentProvider.STRIPE, paymentIntent.getId(), "Subscription"
            );

            PaymentTransaction saved = paymentRecordService.createPendingTransaction(paymentCreatedEvent);
            redisService.cache(paymentCreatedEvent);

            return PaymentCreationResponse.builder()
                    .provider(PaymentProvider.STRIPE)
                    .serverTransactionId(String.valueOf(saved.getId()))
                    .providerTransactionId(paymentIntent.getId())
                    .serverStatus(stripeStatus.getPaymentStatus())
                    .providerPaymentLink(paymentIntent.getClientSecret())
                    .build();
        } catch (StripeException ex) {
            log.error("Failed to process Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment processing failed: " + ex.getMessage());
        }
    }

    @Override
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String paymentId = Optional.ofNullable(paymentData.get("id"))
                .orElseThrow(() -> new StripePaymentException("Missing payment ID"));

        log.info("Verifying Stripe payment: {}", paymentId);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            PaymentTransaction transaction = paymentTransactionRepository.findByProviderTransactionId(paymentId)
                    .orElseThrow(TransactionNotFoundException::new);

            LocalDateTime now = LocalDateTime.now();
            if (transaction.getExpiresAt() != null && now.isAfter(transaction.getExpiresAt())) {
                throw new TransactionExpiredException("Payment expired");
            }

            // Reject replays: only allow transition if currently PENDING
            if (transaction.getStatus() != PaymentStatus.PENDING) {
                log.warn("Ignoring Stripe callback replay for txn {} with status {}", paymentId, transaction.getStatus());
                return new PaymentVerificationResponse(false, paymentId, "ALREADY_FINALIZED", "Transaction is not pending", null, null);
            }

            transaction.setStatus(PaymentStatus.valueOf(stripeStatus.getStatus()));
            // Auto-cancel if expired and still unpaid
            boolean success = stripeStatus == StripeTransactionStatus.SUCCEEDED;
            if (!success && transaction.getExpiresAt() != null
                    && LocalDateTime.now().isAfter(transaction.getExpiresAt())
                    && transaction.getStatus() == PaymentStatus.PENDING) {
                transaction.setStatus(PaymentStatus.CANCELLED);
                transaction.setCancelDate(LocalDateTime.now());
                transaction.setStatus(PaymentStatus.AUTO_CANCELLED);
            } else {
                transaction.setStatus(stripeStatus.getPaymentStatus());
            }
            paymentTransactionRepository.save(transaction);
            String message = paymentI18nService.messageFor(PaymentProvider.STRIPE, stripeStatus.getStatus());

            paymentEventProducer.publishSubscriptionSuccessEvent();

            return new PaymentVerificationResponse(
                    stripeStatus == StripeTransactionStatus.SUCCEEDED,
                    paymentId, stripeStatus.getStatus(),
                    message,
                    null,
                    null
            );
        } catch (StripeException ex) {
            log.error("Failed to verify Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public Object queryPayment(String userId, String paymentId) {
        log.info("Querying Stripe payment with ID: {}", paymentId);
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            PaymentTransaction transaction = paymentTransactionRepository.findByProviderTransactionId(paymentId)
                    .orElseThrow(TransactionNotFoundException::new);

            // Only update DB based on the provider if we haven't finalized internally
            if (transaction.getStatus() == PaymentStatus.PENDING) {
                transaction.setStatus(PaymentStatus.valueOf(stripeStatus.getStatus()));
                transaction.setStatus(stripeStatus.getPaymentStatus());
                paymentTransactionRepository.save(transaction);
            }

            String message = paymentI18nService.messageFor(PaymentProvider.STRIPE, stripeStatus.getStatus());
            return new PaymentVerificationResponse(
                    stripeStatus == StripeTransactionStatus.SUCCEEDED,
                    paymentId,
                    paymentIntent.getDescription(),
                    message,
                    paymentIntent.getCurrency(),
                    stripeStatus.getStatus()
            );
        } catch (StripeException ex) {
            log.error("Failed to query Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment query failed: " + ex.getMessage());
        }
    }

    @Override
    public RefundResponse refundPayment(String userId, String paymentId, HttpServletRequest hsr) {
        log.info("Processing Stripe refund for paymentId: {}", paymentId);

        PaymentTransaction transaction = paymentTransactionRepository.findByProviderTransactionId(paymentId)
                .orElseThrow(TransactionNotFoundException::new);

        try {
            Map<String, Object> params = Map.of("payment_intent", paymentId);
            Refund refund = Refund.create(params);

            StripeTransactionStatus refundStatus = refund.getStatus().equalsIgnoreCase("succeeded")
                    ? StripeTransactionStatus.SUCCEEDED
                    : StripeTransactionStatus.fromStatus(refund.getStatus());

            transaction.setStatus(PaymentStatus.REFUNDED);
            transaction.setStatus(PaymentStatus.valueOf(refund.getStatus()));
            paymentTransactionRepository.save(transaction);

            paymentEventProducer.publishSubscriptionCancelEvent();

            return new RefundResponse(
                refund.getId(), 
                refundStatus.getPaymentStatus().getStatus(),
                refund.getReason(),
                refund.getCurrency(),
                new BigDecimal(refund.getAmount())
            );
        } catch (StripeException ex) {
            log.error("Failed to process Stripe refund...: {}", ex.getMessage());
            throw new StripePaymentException("Refund processing failed: " + ex.getMessage());
        }
    } 
}
