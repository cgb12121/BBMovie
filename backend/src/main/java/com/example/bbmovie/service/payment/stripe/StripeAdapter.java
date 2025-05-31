package com.example.bbmovie.service.payment.stripe;

import com.example.bbmovie.entity.PaymentTransaction;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.StripePaymentException;
import com.example.bbmovie.repository.PaymentTransactionRepository;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.service.payment.PaymentProvider;
import com.example.bbmovie.service.payment.PaymentProviderAdapter;
import com.example.bbmovie.service.payment.PaymentStatus;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service("stripeProvider")
public class StripeAdapter implements PaymentProviderAdapter {

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @SuppressWarnings("unused")
    @Value("${payment.stripe.publishable-key}")
    private String publishableKey;

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;

    @Autowired
    public StripeAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        Stripe.apiKey = secretKey;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        log.info("Processing Stripe payment for order: {}", request.getOrderId());
        User user = userRepository.findById(Long.valueOf(request.getUserId()))
                .orElseThrow(() -> new StripePaymentException("Unknown user: " + request.getUserId() + " in Stripe payment processing"));
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUser(user);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentProvider(PaymentProvider.STRIPE);
        transaction.setPaymentMethod(request.getPaymentMethodId());
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription("Order " + request.getOrderId());

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("amount", request.getAmount().multiply(new BigDecimal(100)).longValue());
            params.put("currency", request.getCurrency());
            params.put("description", "Order " + request.getOrderId());
            params.put("payment_method", request.getPaymentMethodId()); // From client-side Stripe.js
            params.put("confirmation_method", "manual");
            params.put("confirm", true);

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            transaction.setPaymentGatewayId(paymentIntent.getId());
            transaction.setProviderStatus(stripeStatus.getStatus());
            transaction.setStatus(stripeStatus.getPaymentStatus());
            paymentTransactionRepository.save(transaction);

            return new PaymentResponse(
                    paymentIntent.getId(),
                    stripeStatus.getPaymentStatus(),
                    paymentIntent.getClientSecret() // For frontend confirmation
            );
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
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest request) {
        String paymentId = paymentData.get("id");
        log.info("Verifying Stripe payment: {}", paymentId);
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);
            StripeTransactionStatus stripeStatus = StripeTransactionStatus.fromStatus(paymentIntent.getStatus());

            PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId((paymentId))
                    .orElseThrow(() -> new StripePaymentException("Transaction not found: " + paymentId));
            transaction.setProviderStatus(stripeStatus.getStatus());
            transaction.setStatus(stripeStatus.getPaymentStatus());
            paymentTransactionRepository.save(transaction);

            return new PaymentVerification(
                    stripeStatus == StripeTransactionStatus.SUCCEEDED,
                    paymentId
            );
        } catch (StripeException ex) {
            log.error("Failed to verify Stripe payment: {}", ex.getMessage());
            throw new StripePaymentException("Payment verification failed: " + ex.getMessage());
        }
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest request) {
        log.info("Processing Stripe refund for paymentId: {}", paymentId);
        PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + paymentId));

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", paymentId);
            Refund refund = Refund.create(params);

            StripeTransactionStatus refundStatus = "succeeded".equalsIgnoreCase(refund.getStatus())
                    ? StripeTransactionStatus.SUCCEEDED
                    : StripeTransactionStatus.fromStatus(refund.getStatus());

            transaction.setStatus(PaymentStatus.REFUNDED);
            transaction.setProviderStatus(refund.getStatus());
            paymentTransactionRepository.save(transaction);

            return new RefundResponse(
                    refund.getId(),
                    refundStatus.getPaymentStatus().getStatus()
            );
        } catch (StripeException ex) {
            log.error("Failed to process Stripe refund: {}", ex.getMessage());
            transaction.setErrorCode(ex.getCode());
            transaction.setErrorMessage(ex.getMessage());
            paymentTransactionRepository.save(transaction);
            throw new StripePaymentException("Refund processing failed: " + ex.getMessage());
        }
    }
}