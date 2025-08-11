package com.bbmovie.payment.service.paypal;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.PayPalPaymentException;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

import static com.bbmovie.payment.service.paypal.PaypalTransactionStatus.APPROVED;
import static com.bbmovie.payment.service.paypal.PaypalTransactionStatus.COMPLETED;

@Log4j2
@Service("paypal")
public class PayPalAdapter implements PaymentProviderAdapter {

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.mode}")
    private String mode;

    @Value("${payment.paypal.return-url:http://localhost:8080/api/payment/success}")
    private String returnUrl;

    @Value("${payment.paypal.cancel-url:http://localhost:8080/api/payment/cancel}")
    private String cancelUrl;

    private APIContext getApiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }

    @Override
    public PaymentCreationResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        Payment payment = createPayment(request);
        try {
            Payment createdPayment = payment.create(getApiContext());
            PaymentStatus status = createdPayment.getState().equalsIgnoreCase(APPROVED.getStatus())
                    ? PaymentStatus.SUCCEEDED
                    : PaymentStatus.PENDING;
            return new PaymentCreationResponse(
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
    public PaymentVerificationResponse verifyPaymentCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        try {
            String paymentId = paymentData.get("paymentId");
            if (paymentId == null) {
                throw new PayPalPaymentException("Missing paymentId in verification data");
            }
            Payment payment = Payment.get(getApiContext(), paymentId);
            boolean success = payment.getState().equalsIgnoreCase(APPROVED.getStatus());
            return new PaymentVerificationResponse(success, payment.getId(), null, null, null, null);
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
            String status = COMPLETED.getStatus().equals(refund.getState())
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

    private Payment createPayment(Transaction transaction) {
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(returnUrl);
        redirectUrls.setCancelUrl(cancelUrl);

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(Collections.singletonList(transaction));
        payment.setRedirectUrls(redirectUrls);
        return payment;
    }
}
