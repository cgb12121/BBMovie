package com.bbmovie.payment.service.stripe;

import com.bbmovie.payment.config.payment.StripeProperties;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.exception.StripePaymentException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service("stripe")
public class StripeAdapter implements PaymentProviderAdapter {

    private static final String TXN_NOT_FOUND = "Transaction not found: ";

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public StripeAdapter(PaymentTransactionRepository paymentTransactionRepository, StripeProperties properties) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        Stripe.apiKey = properties.getSecretKey();
    }

    @Override
    public PaymentCreationResponse createPaymentRequest(PaymentRequest request, HttpServletRequest httpServletRequest) {
        log.info("Processing Stripe payment for order: {}", request.getOrderId());

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentProvider(PaymentProvider.STRIPE);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Order " + request.getOrderId());

        try {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact());
            params.put("currency", request.getCurrency());
            params.put("description", "Order " + request.getOrderId());
            params.put("confirmation_method", "manual");
            params.put("confirm", true);

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            transaction.setProviderTransactionId(paymentIntent.getId());
            transaction.setStatus(PaymentStatus.valueOf(stripeStatus.getStatus()));
            transaction.setStatus(stripeStatus.getPaymentStatus());
            if (request.getExpiresInMinutes() != null && request.getExpiresInMinutes() > 0) {
                transaction.setExpiresAt(LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()));
            }

            PaymentTransaction saved = paymentTransactionRepository.save(transaction);

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

            PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                    .orElseThrow(() -> new StripePaymentException(TXN_NOT_FOUND + paymentId));

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
            return new PaymentVerificationResponse(
                    stripeStatus == StripeTransactionStatus.SUCCEEDED,
                    paymentId, null,
                    null,
                    null,
                    null
            );
        } catch (StripeException ex) {
            log.error("Failed to verify Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public Object queryPayment(String paymentId) {
        log.info("Querying Stripe payment with ID: {}", paymentId);
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                    .orElseThrow(() -> new StripePaymentException(TXN_NOT_FOUND + paymentId));

            // Only update DB based on the provider if we haven't finalized internally
            if (transaction.getStatus() == PaymentStatus.PENDING) {
                transaction.setStatus(PaymentStatus.valueOf(stripeStatus.getStatus()));
                transaction.setStatus(stripeStatus.getPaymentStatus());
                paymentTransactionRepository.save(transaction);
            }

            return new PaymentVerificationResponse(
                    stripeStatus == StripeTransactionStatus.SUCCEEDED,
                    paymentId,
                    paymentIntent.getDescription(),
                    paymentIntent.getAmount().toString(),
                    paymentIntent.getCurrency(),
                    stripeStatus.getStatus()
            );
        } catch (StripeException ex) {
            log.error("Failed to query Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment query failed: " + ex.getMessage());
        }
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest hsr) {
        log.info("Processing Stripe refund for paymentId: {}", paymentId);

        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                .orElseThrow(() -> new RuntimeException(TXN_NOT_FOUND + paymentId));

        try {
            Map<String, Object> params = Map.of("payment_intent", paymentId);
            Refund refund = Refund.create(params);

            StripeTransactionStatus refundStatus = refund.getStatus().equalsIgnoreCase("succeeded")
                    ? StripeTransactionStatus.SUCCEEDED
                    : StripeTransactionStatus.fromStatus(refund.getStatus());

            transaction.setStatus(PaymentStatus.REFUNDED);
            transaction.setStatus(PaymentStatus.valueOf(refund.getStatus()));
            paymentTransactionRepository.save(transaction);

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
