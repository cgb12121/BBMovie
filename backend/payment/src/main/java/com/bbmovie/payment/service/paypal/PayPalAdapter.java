package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.config.payment.PayPalProperties;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PayPalPaymentException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.bbmovie.payment.service.I18nService;
import com.bbmovie.payment.service.SubscriptionPlanService;
import com.bbmovie.payment.service.PricingService;
import com.bbmovie.payment.service.PaymentRecordService;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.bbmovie.payment.service.paypal.PaypalTransactionStatus.*;

@Log4j2
@Service("paypal")
public class PayPalAdapter implements PaymentProviderAdapter {
    
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PayPalProperties properties;
    private final I18nService i18nService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PricingService pricingService;
    private final PaymentRecordService paymentRecordService;

    @Autowired
    public PayPalAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            PayPalProperties properties, I18nService i18nService,
            SubscriptionPlanService subscriptionPlanService,
            PricingService pricingService,
            PaymentRecordService paymentRecordService
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.properties = properties;
        this.i18nService = i18nService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.pricingService = pricingService;
        this.paymentRecordService = paymentRecordService;
    }

    @PostConstruct
    private APIContext getApiContext() {
        return new APIContext(properties.getClientId(), properties.getClientSecret(), properties.getMode());
    }

    @Override
    @Transactional
    public PaymentCreationResponse createPaymentRequest(String userId, SubscriptionPaymentRequest request, HttpServletRequest httpServletRequest) {
        SubscriptionPlan plan = subscriptionPlanService.getById(java.util.UUID.fromString(request.subscriptionPlanId()));

        BigDecimal amountInBaseCurrency = pricingService.calculateFinalBasePrice(
                plan,
                request.billingCycle(),
                userId,
                request.voucherCode()
        );
        Payment payment = createTransaction(new PaymentRequest(null, amountInBaseCurrency, plan.getBaseCurrency().getCurrencyCode(), null, null, null));
        log.info("Created PayPal payment {}", payment.toJSON());
        try {
            Payment createdPayment = payment.create(getApiContext());
            log.info("Created PayPal payment {}", createdPayment.toJSON());

            String approvalUrl = null;
            for (Links link : createdPayment.getLinks()) {
                if ("approval_url".equals(link.getRel())) {
                    approvalUrl = link.getHref();
                    break;
                }
            }

            if (approvalUrl == null) {
                throw new PayPalPaymentException("Approval URL not found in PayPal response");
            }

            PaymentStatus status = createdPayment.getState().equalsIgnoreCase(APPROVED.getStatus())
                    ? PaymentStatus.SUCCEEDED
                    : PaymentStatus.PENDING;

            PaymentTransaction saved = paymentRecordService.createPendingTransaction(
                    userId,
                    plan,
                    amountInBaseCurrency,
                    plan.getBaseCurrency(),
                    PaymentProvider.PAYPAL,
                    createdPayment.getId(),
                    "Subscription"
            );

            return PaymentCreationResponse.builder()
                    .provider(PaymentProvider.PAYPAL)
                    .serverTransactionId(String.valueOf(saved.getId()))
                    .providerTransactionId(createdPayment.getId())
                    .serverStatus(status)
                    .providerPaymentLink(approvalUrl)
                    .build();
        } catch (PayPalRESTException e) {
            log.error("Unable to create PayPal payment", e);
            throw new PayPalPaymentException(e.getDetails());
        }
    }


    @Override
    @Transactional
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        try {
            String paymentId = paymentData.get("paymentId");
            if (paymentId == null) {
                throw new PayPalPaymentException("Missing paymentId in verification data");
            }
            // Shows details for a payment, by ID.
            Payment payment = Payment.get(getApiContext(), paymentId);
            log.info("Got payment details from PayPal {}", payment.toJSON());

            String stateOfPayment = payment.getState();
            boolean success = stateOfPayment.equalsIgnoreCase(COMPLETED.getStatus());

            String messageForClient = getMessage(stateOfPayment);
            
            PaymentTransaction transaction = paymentTransactionRepository.findByProviderTransactionId(paymentId)
                    .orElseThrow(() -> new PayPalPaymentException("Transaction not found: " + paymentId));

            LocalDateTime now = LocalDateTime.now();
            if (transaction.getExpiresAt() != null && now.isAfter(transaction.getExpiresAt())) {
                throw new TransactionExpiredException("Payment expired");
            }

            // Reject replays: only allow update when still pending
            if (transaction.getStatus() != com.bbmovie.payment.entity.enums.PaymentStatus.PENDING) {
                return PaymentVerificationResponse.builder()
                        .isValid(false)
                        .transactionId(payment.getId())
                        .code("ALREADY_FINALIZED")
                        .message("Transaction is not pending")
                        .clientResponse(payment.getTransactions())
                        .providerResponse(null)
                        .build();
            }

            if (!transaction.getStatus().getStatus().equalsIgnoreCase(stateOfPayment)) {
                transaction.setStatus(PaymentStatus.valueOf(stateOfPayment));
                paymentTransactionRepository.save(transaction);
            }

            return PaymentVerificationResponse.builder()
                    .isValid(success)
                    .transactionId(payment.getId())
                    .code(payment.getState())
                    .message(messageForClient)
                    .clientResponse(payment.getTransactions())
                    .providerResponse(null)
                    .build();
        } catch (PayPalRESTException e) {
            log.error("Unable to verify PayPal payment", e);
            throw new PayPalPaymentException("Unable to verify PayPal payment");
        }
    }

    @Override
    public PaymentVerificationResponse handleWebhook(CallbackRequestContext ctx) {
        Event event = null;
        try {
            APIContext apiContext = getApiContext();

            boolean isValid = Event.validateReceivedEvent(apiContext, ctx.getHeaders(), ctx.getRawBody());

            if (isValid) {
                event = Event.get(apiContext, ctx.getRawBody());
                log.info("Got webhook event {}", event.toJSON());
            }

            return new PaymentVerificationResponse(
                    isValid,
                    event != null ? event.getId() : null,
                    event != null ? event.getEventType() : null,
                    event != null ? event.getSummary() : "Invalid webhook signature",
                    ctx.getRawBody(),
                    null);

        } catch (PayPalRESTException e) {
            log.error("Error validating PayPal webhook", e);
            return new PaymentVerificationResponse(
                    false,
                    null,
                    null,
                    e.getMessage(),
                    ctx.getRawBody(),
                    null);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new PayPalPaymentException("Webhook signature validation error");
        }
    }

    @Override
    public Object queryPayment(String userId, String paymentId) {
        try {
            PaymentTransaction transaction = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                    .orElseThrow(() -> new PayPalPaymentException("Transaction not found: " + paymentId));

            Payment payment = Payment.get(getApiContext(), transaction.getProviderTransactionId());
            log.info("Queried payment details from PayPal {}", payment.toJSON());
            return payment;
        } catch (PayPalRESTException e) {
            log.error("Unable to query PayPal payment", e);
            throw new PayPalPaymentException("Unable to query PayPal payment");
        }
    }

    @Override
    public RefundResponse refundPayment(String userId, String paymentId, HttpServletRequest hsr) {
        try {
            PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                        .orElseThrow(() -> new PayPalPaymentException("Unable to find transaction"));

            Sale sale = Sale.get(getApiContext(), txn.getProviderTransactionId());

            RefundRequest refundRequest = new RefundRequest();

            if (txn.getAmount() != null) {
                Amount amount = new Amount();
                amount.setCurrency(txn.getCurrency().getCurrencyCode());
                amount.setTotal(txn.getAmount().toString());
                refundRequest.setAmount(amount);
            }

            DetailedRefund refund = sale.refund(getApiContext(), refundRequest);
            String status = COMPLETED.getStatus().equals(refund.getState())
                    ? PaymentStatus.SUCCEEDED.getStatus()
                    : PaymentStatus.FAILED.getStatus();
            return new RefundResponse(
                refund.getId(), 
                status, 
                refund.getReasonCode(),
                refund.getTotalRefundedAmount().getCurrency(), 
                new BigDecimal(refund.getTotalRefundedAmount().getValue())
            );
        } catch (PayPalRESTException e) {
            log.error("Unable to refund PayPal payment", e);
            throw new PayPalPaymentException("Unable to refund PayPal payment");
        }
    }

    private Payment createTransaction(PaymentRequest request) {
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(request.getAmount() != null ? request.getAmount().toString() : null);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Subscription");

        return createTransactionDetails(transaction);
    }

    
    private Payment createTransactionDetails(Transaction transaction) {
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(properties.getReturnUrl());
        redirectUrls.setCancelUrl(properties.getCancelUrl());

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Collections.singletonList(transaction));
        payment.setRedirectUrls(redirectUrls);
        return payment;
    }

    private String getMessage(String stateOfPayment) {
        if (stateOfPayment.equalsIgnoreCase(COMPLETED.getStatus())) {
            return i18nService.getMessage("payment.paypal.completed");
        }
        if (stateOfPayment.equalsIgnoreCase(APPROVED.getStatus())) {
            return i18nService.getMessage("payment.paypal.pending");
        }
        if (stateOfPayment.equalsIgnoreCase(FAILED.getStatus())) {
            return i18nService.getMessage("payment.paypal.failed");
        }
        return i18nService.getMessage("payment.paypal.unknown");
    }
}
