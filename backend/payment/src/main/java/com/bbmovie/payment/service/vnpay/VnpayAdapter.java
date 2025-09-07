package com.bbmovie.payment.service.vnpay;

import com.bbmovie.payment.config.payment.VnpayProperties;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.exception.TransactionExpiredException;
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

import static com.bbmovie.payment.service.vnpay.VnpayQueryParams.*;
import static com.bbmovie.payment.service.vnpay.VnpayTransactionStatus.SUCCESS;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.stringToJsonNode;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.toJsonString;

@Log4j2
@Service("vnpay")
public class VnpayAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VnpayProvidedFunction vnpayProvidedFunction;
    private final VnpayProperties properties;

    @Autowired
    public VnpayAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            VnpayProvidedFunction vnpayProvidedFunction,
            VnpayProperties properties) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.vnpayProvidedFunction = vnpayProvidedFunction;
        this.properties = properties;
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
        String vnpTxnRef = getRandomNumber(16);
        String paymentUrl = vnpayProvidedFunction.createOrder(
                httpServletRequest, amountStr , vnpTxnRef, "billpayment",
                properties.getTmnCode(), properties.getReturnUrl(), properties.getHashSecret(), properties.getPayUrl()
        );

        PaymentTransaction transaction = createTransactionForVnpay(request, vnpTxnRef, paymentUrl);
        if (request.getExpiresInMinutes() != null && request.getExpiresInMinutes() > 0) {
            transaction.setExpiresAt(LocalDateTime.now().plusMinutes(request.getExpiresInMinutes()));
        }
        PaymentTransaction saved = paymentTransactionRepository.save(transaction);

        return PaymentCreationResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .serverTransactionId(String.valueOf(saved.getId()))
                .providerTransactionId(vnpTxnRef)
                .serverStatus(PaymentStatus.PENDING)
                .providerPaymentLink(paymentUrl)
                .build();
    }

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

        String calculateChecksum = vnpayProvidedFunction.hashAllFields(paymentData, properties.getHashSecret());

        String responseCode = paymentData.get(VNPAY_RESPONSE_CODE);
        String message = VnpayTransactionStatus.getMessageFromCode(responseCode);

        boolean isValid = checkSum.equals(calculateChecksum) && SUCCESS.getCode().equals(responseCode);

        paymentTransactionRepository.findByPaymentGatewayId(vnpTxnRef).ifPresent(tx -> {
            LocalDateTime now = LocalDateTime.now();
            if (tx.getExpiresAt() != null && now.isAfter(tx.getExpiresAt())) {
                throw new TransactionExpiredException("Payment expired");
            }

            tx.setLastModifiedDate(now);
            // Only process if still pending; prevent replay from flipping canceled/refunded/succeeded
            if (tx.getStatus() != PaymentStatus.PENDING) {
                return;
            }

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
        Map<String, String> body = vnpayProvidedFunction.createQueryOrder(httpServletRequest, txn, properties.getTmnCode(), properties.getHashSecret());
        log.info("Querying VNPay payment: {}", body);
        return vnpayProvidedFunction.executeRequest(body, properties.getApiUrl(), properties.getHashSecret());
    }

    @Override
    @Transactional
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Map<String, String> body = vnpayProvidedFunction.createRefundOrder(httpServletRequest, txn, properties.getTmnCode(), properties.getHashSecret());
        Map<String, String> result = vnpayProvidedFunction.executeRequest(body, properties.getApiUrl(), properties.getHashSecret());
        return new RefundResponse(result.get(VNPAY_TXN_REF_PARAM), result.get(VNPAY_RESPONSE_CODE));
    }

    private PaymentTransaction createTransactionForVnpay(PaymentRequest request, String vnpTxnRef, String paymentUrl) {
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
                .ipnUrl(properties.getReturnUrl())
                .returnUrl(paymentUrl)
                .build();
    }

    @SuppressWarnings("squid:S2119")
    public String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}