package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.config.payment.PayPalProperties;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PayPalPaymentException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
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

    @Autowired
    public PayPalAdapter(PaymentTransactionRepository paymentTransactionRepository, PayPalProperties properties) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.properties = properties;
    }

    private APIContext getApiContext() {
        return new APIContext(properties.getClientId(), properties.getClientSecret(), properties.getMode());
    }

    @Override
    @Transactional
    public PaymentCreationResponse createPaymentRequest(PaymentRequest request, HttpServletRequest httpServletRequest) {
        Payment payment = createPayment(request);
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

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .userId(request.getUserId())
                    .subscription(null)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .paymentProvider(PaymentProvider.PAYPAL)
                    .providerStatus(status.getStatus())
                    .returnUrl(approvalUrl)
                    .paymentGatewayOrderId(createdPayment.getId())
                    .build();
            if (request.getExpiresInMinutes() != null && request.getExpiresInMinutes() > 0) {
                transaction.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()));
            }
            
            PaymentTransaction saved = paymentTransactionRepository.save(transaction);

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
            
            PaymentTransaction transaction = paymentTransactionRepository.findByPaymentGatewayId(paymentId)
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

            if (!transaction.getProviderStatus().equalsIgnoreCase(stateOfPayment)) {
                transaction.setProviderStatus(stateOfPayment);
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
    public Object queryPayment(String paymentId) {
        try {
            PaymentTransaction transaction = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                    .orElseThrow(() -> new PayPalPaymentException("Transaction not found: " + paymentId));

            Payment payment = Payment.get(getApiContext(), transaction.getPaymentGatewayOrderId());
            log.info("Queried payment details from PayPal {}", payment.toJSON());
            return payment;
        } catch (PayPalRESTException e) {
            log.error("Unable to query PayPal payment", e);
            throw new PayPalPaymentException("Unable to query PayPal payment");
        }
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest hsr) {
        try {
            PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                        .orElseThrow(() -> new PayPalPaymentException("Unable to find transaction"));

            Sale sale = Sale.get(getApiContext(), txn.getId().toString());

            RefundRequest refundRequest = new RefundRequest();

            if (txn.getAmount() != null) {
                Amount amount = new Amount();
                amount.setCurrency(txn.getCurrency());
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

    /*
         Init object for request:
         
          {
             "intent": The payment intent from the request,
              "payer": {
                  "payment_method": "paypal"
               },
               "transactions": [
                  {
                   "amount": {
                      "currency": "[...]",
                      "total": "[...]"
                     },
                    "description": "[...]"
                   }
                 ],
                "redirect_urls": {
                "return_url": "[...]",
                "cancel_url": "[...]"
            }
         }
    */
    private Payment createPayment(PaymentRequest request) {
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(request.getAmount() != null ? request.getAmount().toString() : null);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Order " + request.getOrderId());

        return createPayment(transaction);
    }

    
    private Payment createPayment(Transaction transaction) {
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

    /*
        Execute request to PayPal: Payment.create(getApiContext())
    
        Return object:
        {
          "id": A unique string identifier for the payment,
          "intent": The payment intent from the request, 
          "payer": {
            "payment_method": "paypal"
          },
          "transactions": [
            {
              "related_resources": [],
              "amount": {
                "currency": "[...]",
                "total": "[...]"
              },
              "description": "[...]"
            }
          ],
          "state": "created",
          "create_time": "[ISO 8601]",
          "links": [
            {
              "href": "https://api.sandbox.paypal.com/v1/payments/payment/{id}",
              "rel": "self",
              "method": "GET"

                 This is an API link to retrieve (GET) the current payment details from PayPal's servers.
                 It's for your backend to check or update the payment status later.
                 Not for the user.

            },
            {
                "href":"https://www.sandbox.paypal.com/cgi-bin/webscr?cmd[what are these here for?]",
                "rel":"approval_url",
                "method":"REDIRECT"

                         This is the link you should use for the user.
                         Redirect the user to this URL (via a browser or app) to approve and complete the
                         payment on PayPal's website.

            },
            {
                "href":"https://api.sandbox.paypal.com/v1/payments/payment/{id}/execute",
                "rel":"execute",
                "method":"POST"

                         This is an API link to execute (POST) the payment after user approval.
                         Your backend calls this to finalize the transaction once the user returns from the approval page.
                         Not for the user.
            }
          ]
       }
    */
    private static String getMessage(String stateOfPayment) {
        String messageForClient = "";
        if (stateOfPayment.equalsIgnoreCase(COMPLETED.getStatus())) {
            messageForClient = "The payment is completed, please check your order.";
        }
        if (stateOfPayment.equalsIgnoreCase(APPROVED.getStatus())) {
            messageForClient = "The payment is pending, please check your order.";
        }
        if (stateOfPayment.equalsIgnoreCase(FAILED.getStatus())) {
            messageForClient = "The payment is failed. if your balance has been deducted, please contact the admin or PayPal hotline for support.";
        }
        return messageForClient;
    }
}
