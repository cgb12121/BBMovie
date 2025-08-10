package com.bbmovie.payment.service.vnpay;

import com.bbmovie.payment.dto.PaymentRequest;
import com.bbmovie.payment.dto.PaymentResponse;
import com.bbmovie.payment.dto.PaymentVerification;
import com.bbmovie.payment.dto.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.bbmovie.payment.service.vnpay.VnpayProvidedFunction.hashAllFields;
import static com.bbmovie.payment.service.vnpay.VnpayQueryParams.*;
import static com.bbmovie.payment.service.vnpay.VnpayTransactionStatus.SUCCESS;

@Log4j2
@Service("vnpay")
public class VnpayAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public VnpayAdapter(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amountInVnd = request.getAmount();
        String amountStr = amountInVnd.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        String vnpTxnRef = VnpayProvidedFunction.getRandomNumber(16);
        String paymentUrl = VnpayProvidedFunction.createOrder(httpServletRequest, amountStr , vnpTxnRef, "billpayment");

        PaymentTransaction transaction = createTransactionForVnpay(request, vnpTxnRef, paymentUrl);
        paymentTransactionRepository.save(transaction);

        return new PaymentResponse(vnpTxnRef, PaymentStatus.PENDING, paymentUrl);
    }

    @Override
    @Transactional
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String checkSum = paymentData.get(VNPAY_SECURE_HASH);
        String vnpTxnRef = paymentData.get(VNPAY_TXN_REF_PARAM);
        String cardType = paymentData.get(VNPAY_CARD_TYPE);
        String bankCode = paymentData.get(VNPAY_BANK_CODE);
        paymentData.remove(VNPAY_SECURE_HASH);

        String vpnTransactionNo = paymentData.get(VNPAY_TRANSACTION_NO);

        String paymentMethod = switch (cardType) {
            case "ATM" -> "Domestic card (" + bankCode + ")";
            case "VISA" -> "VISA card (" + bankCode + ")";
            case "MASTERCARD" -> "Mastercard card (" + bankCode + ")";
            case "JCB" -> "JCB card (" + bankCode + ")";
            case "AMEX" -> "AMEX card (" + cardType + ")";
            case "QR" -> "QR code payment (" + cardType + ")";
            case "VNPAYQR" -> "Vnpay QR code payment (" + bankCode + ")";
            default -> "Unknown (" + cardType + ", " + bankCode + ")";
        };

        String calculateChecksum = hashAllFields(paymentData);

        String responseCode = paymentData.get(VNPAY_RESPONSE_CODE);
        String message = VnpayTransactionStatus.getMessageFromCode(responseCode);

        boolean isValid = checkSum.equals(calculateChecksum) && SUCCESS.getCode().equals(responseCode);

        paymentTransactionRepository.findByPaymentGatewayId(vnpTxnRef).ifPresent(tx -> {
            tx.setLastModifiedDate(LocalDateTime.now());
            if (isValid) {
                tx.setStatus(PaymentStatus.SUCCEEDED);
                tx.setPaymentMethod(paymentMethod);
                tx.setPaymentGatewayOrderId(vpnTransactionNo);
                tx.setProviderStatus(paymentData.get(VNPAY_RESPONSE_CODE));
            } else {
                tx.setStatus(PaymentStatus.FAILED);
                tx.setErrorCode(responseCode);
                tx.setErrorMessage(VnpayTransactionStatus.getMessageFromCode(responseCode));
            }
            paymentTransactionRepository.save(tx);
        });

        return new PaymentVerification(isValid, vnpTxnRef, responseCode, message);
    }

    @Override
    public Object queryPaymentFromProvider(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Map<String, String> body = VnpayProvidedFunction.createQueryOrder(httpServletRequest, txn);
        log.info("Querying VNPay payment: {}", body);
        return VnpayProvidedFunction.executeRequest(body);
    }

    @Override
    @Transactional
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Map<String, String> body = VnpayProvidedFunction.createRefundOrder(httpServletRequest, txn);
        Map<String, String> result = VnpayProvidedFunction.executeRequest(body);
        return new RefundResponse(result.get(VNPAY_TXN_REF_PARAM), result.get(VNPAY_RESPONSE_CODE));
    }

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.VNPAY;
    }

    private static PaymentTransaction createTransactionForVnpay(PaymentRequest request, String vnpTxnRef, String paymentUrl) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(request.getUserId());
        transaction.setSubscription(null); // or set if linked to a subscription later
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentProvider(PaymentProvider.VNPAY);
        transaction.setPaymentGatewayId(vnpTxnRef); // store gateway transaction reference
        transaction.setProviderStatus("PENDING");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setDescription("VNPay payment for order: " + request.getOrderId());
        transaction.setIpnUrl("http://localhost:8088/api/payment/vnpay/callback");
        transaction.setReturnUrl(paymentUrl);
        return transaction;
    }
}