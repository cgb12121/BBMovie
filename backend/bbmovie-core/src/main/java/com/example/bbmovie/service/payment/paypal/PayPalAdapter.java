package com.example.bbmovie.service.payment.paypal;

import com.example.bbmovie.exception.PayPalPaymentException;
import com.example.bbmovie.service.payment.PaymentProviderAdapter;
import com.example.bbmovie.entity.enumerate.PaymentStatus;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service("paypalProvider")
@Log4j2(topic = "PayPalAdapter")
public class PayPalAdapter implements PaymentProviderAdapter {

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.mode}")
    private String mode;

    private APIContext getApiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        Payment payment = getPayment(request);

        try {
            Payment createdPayment = payment.create(getApiContext());
            return new PaymentResponse(
                    createdPayment.getId(),
                    createdPayment.getState().equalsIgnoreCase(PaypalTransactionStatus.APPROVED.getStatus())
                            ? PaymentStatus.SUCCEEDED
                            : PaymentStatus.PENDING,
                    createdPayment.getId()
            );
        } catch (PayPalRESTException e) {
            log.error("Unable to create PayPAL payment: ", e);
            throw new PayPalPaymentException("Unable to create PayPal Payment");
        }
    }

    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        try {
            String paymentId = paymentData.get("paymentId");
            Payment payment = Payment.get(getApiContext(), paymentId);
            return new PaymentVerification(
                    payment.getState().equalsIgnoreCase(PaypalTransactionStatus.APPROVED.getStatus()),
                    payment.getId()
            );
        } catch (PayPalRESTException e) {
            log.error("Unable to verify PayPAL payment: ", e);
            throw new PayPalPaymentException("Unable to verify PayPAL payment");
        }
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        try {
            Sale sale = Sale.get(getApiContext(), paymentId);
            RefundRequest refundRequest = new RefundRequest();
            DetailedRefund refund = sale.refund(getApiContext(), refundRequest);
            return new RefundResponse(
                    refund.getId(),
                    refund.getState().equals(PaypalTransactionStatus.COMPLETED.getStatus())
                            ? PaymentStatus.SUCCEEDED.getStatus()
                            : PaymentStatus.FAILED.getStatus()
            );
        } catch (PayPalRESTException e) {
            log.error("Unable to refund PayPal payment: ", e);
            throw new PayPalPaymentException("Unable to refund PayPal payment.");
        }
    }

    @NotNull
    private static Payment getPayment(PaymentRequest request) {
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(request.getAmount().toString());

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setDescription("Order " + request.getOrderId());

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);

        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl("http://localhost:8080/api/payment/success");
        redirectUrls.setCancelUrl("http://localhost:8080/api/payment/cancel");
        payment.setRedirectUrls(redirectUrls);
        return payment;
    }
}