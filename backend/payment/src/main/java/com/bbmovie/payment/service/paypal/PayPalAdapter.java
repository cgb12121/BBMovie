package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PayPalPaymentException;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service("paypal")
public class PayPalAdapter implements PaymentProviderAdapter {

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.mode}")
    private String mode;

    private static final Logger log = LoggerFactory.getLogger(PayPalAdapter.class);

    private APIContext getApiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        Payment payment = createPayment(request);
        try {
            Payment createdPayment = payment.create(getApiContext());
            PaymentStatus status = createdPayment.getState().equalsIgnoreCase(PaypalTransactionStatus.APPROVED.getStatus())
                    ? PaymentStatus.SUCCEEDED
                    : PaymentStatus.PENDING;
            return new PaymentResponse(
                    createdPayment.getId(),
                    status,
                    createdPayment.getId()
            );
        } catch (PayPalRESTException e) {
            log.error("Unable to create PayPal payment", e);
            throw new PayPalPaymentException("Unable to create PayPal payment");
        }
    }

    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        try {
            String paymentId = paymentData.get("paymentId");
            if (paymentId == null) {
                throw new PayPalPaymentException("Missing paymentId in verification data");
            }
            Payment payment = Payment.get(getApiContext(), paymentId);
            boolean success = payment.getState().equalsIgnoreCase(PaypalTransactionStatus.APPROVED.getStatus());
            return new PaymentVerification(success, payment.getId(), null, null);
        } catch (PayPalRESTException e) {
            log.error("Unable to verify PayPal payment", e);
            throw new PayPalPaymentException("Unable to verify PayPal payment");
        }
    }

    @Override
    public Object queryPaymentFromProvider(String paymentId, HttpServletRequest httpServletRequest) {
        return null;
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        try {
            Sale sale = Sale.get(getApiContext(), paymentId);
            RefundRequest refundRequest = new RefundRequest();
            DetailedRefund refund = sale.refund(getApiContext(), refundRequest);
            String status = PaypalTransactionStatus.COMPLETED.getStatus().equals(refund.getState())
                    ? PaymentStatus.SUCCEEDED.getStatus()
                    : PaymentStatus.FAILED.getStatus();
            return new RefundResponse(refund.getId(), status);
        } catch (PayPalRESTException e) {
            log.error("Unable to refund PayPal payment", e);
            throw new PayPalPaymentException("Unable to refund PayPal payment");
        }
    }

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.PAYPAL;
    }

    private Payment createPayment(PaymentRequest request) {
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(request.getAmount() != null ? request.getAmount().toString() : null);

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Order " + request.getOrderId());

        return createPayment(transaction);
    }

    @NotNull
    private static Payment createPayment(Transaction transaction) {
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl("http://localhost:8080/api/payment/success");
        redirectUrls.setCancelUrl("http://localhost:8080/api/payment/cancel");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Collections.singletonList(transaction));
        payment.setRedirectUrls(redirectUrls);
        return payment;
    }
}
