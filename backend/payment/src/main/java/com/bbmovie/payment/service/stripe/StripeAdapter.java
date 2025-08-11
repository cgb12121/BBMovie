package com.bbmovie.payment.service.stripe;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.exception.StripePaymentException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service("stripe")
public class StripeAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @SuppressWarnings("unused")
    @Value("${payment.stripe.publishable-key}")
    private String publishableKey;

    @Autowired
    public StripeAdapter(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        Stripe.apiKey = secretKey;
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
            Map<String, Object> params = Map.of(
                    "amount", request.getAmount().multiply(BigDecimal.valueOf(100)).longValueExact(),
                    "currency", request.getCurrency(),
                    "description", "Order " + request.getOrderId(),
                    "confirmation_method", "manual",
                    "confirm", true
            );

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            transaction.setPaymentGatewayId(paymentIntent.getId());
            transaction.setProviderStatus(stripeStatus.getStatus());
            transaction.setStatus(stripeStatus.getPaymentStatus());

            paymentTransactionRepository.save(transaction);

            return new PaymentCreationResponse(paymentIntent.getId(), stripeStatus.getPaymentStatus(), paymentIntent.getClientSecret());
        } catch (StripeException ex) {
            log.error("Failed to process Stripe payment: {}", ex.getMessage());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setErrorCode(ex.getCode());
            transaction.setErrorMessage(ex.getMessage());
            paymentTransactionRepository.save(transaction);
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
                    .orElseThrow(() -> new StripePaymentException("Transaction not found: " + paymentId));

            transaction.setProviderStatus(stripeStatus.getStatus());
            transaction.setStatus(stripeStatus.getPaymentStatus());
            paymentTransactionRepository.save(transaction);

            return new PaymentVerificationResponse(stripeStatus == StripeTransactionStatus.SUCCEEDED, paymentId, null, null, null, null);
        } catch (StripeException ex) {
            log.error("Failed to verify Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        return null;
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        log.info("Processing Stripe refund for paymentId: {}", paymentId);

        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + paymentId));

        try {
            Map<String, Object> params = Map.of("payment_intent", paymentId);
            Refund refund = Refund.create(params);

            StripeTransactionStatus refundStatus = refund.getStatus().equalsIgnoreCase("succeeded")
                    ? StripeTransactionStatus.SUCCEEDED
                    : StripeTransactionStatus.fromStatus(refund.getStatus());

            transaction.setStatus(PaymentStatus.REFUNDED);
            transaction.setProviderStatus(refund.getStatus());
            paymentTransactionRepository.save(transaction);

            return new RefundResponse(refund.getId(), refundStatus.getPaymentStatus().getStatus());
        } catch (StripeException ex) {
            log.error("Failed to process Stripe refund: {}", ex.getMessage());
            transaction.setErrorCode(ex.getCode());
            transaction.setErrorMessage(ex.getMessage());
            paymentTransactionRepository.save(transaction);
            throw new StripePaymentException("Refund processing failed: " + ex.getMessage());
        }
    }
}
