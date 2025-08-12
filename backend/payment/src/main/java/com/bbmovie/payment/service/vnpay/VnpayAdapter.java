package com.bbmovie.payment.service.vnpay;

import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.VNPayException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.fasterxml.jackson.databind.JsonNode;
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
import static com.bbmovie.payment.service.vnpay.VnpayProvidedFunction.vnp_Returnurl;
import static com.bbmovie.payment.service.vnpay.VnpayQueryParams.*;
import static com.bbmovie.payment.service.vnpay.VnpayTransactionStatus.SUCCESS;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.stringToJsonNode;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.toJsonString;

@Log4j2
@Service("vnpay")
public class VnpayAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public VnpayAdapter(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public PaymentCreationResponse createPaymentRequest(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amountInVnd = request.getAmount();
        String currency = request.getCurrency();
        if (currency == null || !currency.equalsIgnoreCase("VND")) {
            throw new VNPayException("Vnpay only support VND currency");
        }

        String amountStr = amountInVnd.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        String vnpTxnRef = VnpayProvidedFunction.getRandomNumber(16);
        String paymentUrl = VnpayProvidedFunction.createOrder(httpServletRequest, amountStr , vnpTxnRef, "billpayment");

        PaymentTransaction transaction = createTransactionForVnpay(request, vnpTxnRef, paymentUrl);
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        return PaymentCreationResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .serverTransactionId(String.valueOf(saved.getId()))
                .providerTransactionId(vnpTxnRef)
                .serverStatus(PaymentStatus.PENDING)
                .providerPaymentLink(paymentUrl)
                .build();
    }

    //should prevent process payment again after callback
    @Override
    @Transactional
    public PaymentVerificationResponse handleCallback(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
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

        JsonNode providerData = stringToJsonNode(toJsonString(paymentData));

        return PaymentVerificationResponse.builder()
                .isValid(isValid)
                .transactionId(vpnTransactionNo)
                .code(responseCode)
                .message(message)
                .providerPayloadStringJson(providerData)
                .responseToProviderStringJson("No response to VNPay")
                .build();
    }

    @Override
    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
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

    private static PaymentTransaction createTransactionForVnpay(PaymentRequest request, String vnpTxnRef, String paymentUrl) {
        return PaymentTransaction.builder()
                .userId(request.getUserId())
                .subscription(null)  // or set if linked to a subscription later
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentProvider(PaymentProvider.VNPAY)
                .paymentGatewayOrderId(vnpTxnRef) // store gateway transaction reference
                .providerStatus("PENDING")
                .transactionDate(LocalDateTime.now())
                .status(PaymentStatus.PENDING)
                .description("VNPay payment for order: " + request.getOrderId())
                .ipnUrl(vnp_Returnurl)
                .returnUrl(paymentUrl)
                .build();
    }
}