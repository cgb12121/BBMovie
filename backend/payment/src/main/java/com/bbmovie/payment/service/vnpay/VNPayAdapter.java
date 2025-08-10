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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.bbmovie.payment.service.vnpay.VNPayUtils.hashAllFields;
import static com.bbmovie.payment.service.vnpay.VnPayQueryParams.*;

@Log4j2
@Service("vnpayProvider")
public class VNPayAdapter implements PaymentProviderAdapter {

    @Value("${payment.vnpay.TmnCode}")
    private String tmnCode;

    @Value("${payment.vnpay.HashSecret}")
    private String hashSecret;

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public VNPayAdapter(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        BigDecimal amountInVnd = request.getAmount();
        String amountStr = amountInVnd.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        String vnpTxnRef = VNPayUtils.getRandomNumber(16);
        String paymentUrl = VNPayUtils.createOrder(
                httpServletRequest, amountStr , vnpTxnRef, "billpayment",
                "http:localhost:8088/api/payment/vnpay/callback"
        );

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setUserId(request.getUserId());
        transaction.setSubscription(null); // or set if linked to subscription
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setPaymentProvider(PaymentProvider.VNPAY);
        transaction.setPaymentMethod(request.getPaymentMethodId());
        transaction.setPaymentGatewayId(vnpTxnRef); // store gateway transaction reference
        transaction.setProviderStatus("PENDING");
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(PaymentStatus.PENDING);
        transaction.setDescription("VNPay payment for order: " + request.getOrderId());
        transaction.setIpnUrl("http://localhost:8088/api/payment/vnpay/callback");
        transaction.setReturnUrl(paymentUrl);

        paymentTransactionRepository.save(transaction);

        return new PaymentResponse(vnpTxnRef, PaymentStatus.PENDING, paymentUrl);
    }

    @Override
    public PaymentVerification verifyPayment(Map<String, String> paymentData, HttpServletRequest httpServletRequest) {
        String vnpSecureHash = paymentData.get(VNPAY_SECURE_HASH);
        String vnpTxnRef = paymentData.get(VNPAY_TXN_REF_PARAM);
        String cardType = paymentData.get("vnp_CardType");
        String bankCode = paymentData.get("vnp_BankCode");
        paymentData.remove(VNPAY_SECURE_HASH);

        String vpnTransactionNo = paymentData.get("vnp_TransactionNo");

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

        String calculatedHash = hashAllFields(paymentData);
        boolean isValid = vnpSecureHash.equals(calculatedHash)
                && VnPayTransactionStatus.SUCCESS.getCode().equals(paymentData.get(VNPAY_RESPONSE_CODE));

        paymentTransactionRepository.findByPaymentGatewayId(vnpTxnRef).ifPresent(tx -> {
            tx.setLastModifiedDate(LocalDateTime.now());
            if (isValid) {
                tx.setStatus(PaymentStatus.SUCCEEDED);
                tx.setPaymentMethod(paymentMethod);
                tx.setPaymentGatewayOrderId(vpnTransactionNo);
                tx.setProviderStatus(paymentData.get(VNPAY_RESPONSE_CODE));
            } else {
                tx.setStatus(PaymentStatus.FAILED);
                tx.setErrorCode(paymentData.get(VNPAY_RESPONSE_CODE));
                tx.setErrorMessage("Signature mismatch or VNPay error");
            }
            paymentTransactionRepository.save(tx);
        });

        return new PaymentVerification(isValid, vnpTxnRef);
    }

    @SuppressWarnings("all")
    @Override
    public Object queryPayment(String paymentId, HttpServletRequest httpServletRequest) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        String vnpTxnRef = txn.getPaymentGatewayId();
        String transactionNo = txn.getPaymentGatewayOrderId();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnpTransactionDate = txn.getTransactionDate().format(formatter);

        String vnpRequestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String vnpVersion = "2.1.0";
        String vnpCommand = "querydr";
        String vnpIpAddr = VNPayUtils.getIpAddress(httpServletRequest);
        String vnpCreateDate = formatter.format(LocalDateTime.now());
        String vnpOrderInfo = "Query transaction " + vnpTxnRef;

        String data = String.join("|",
                vnpRequestId, vnpVersion, vnpCommand, VNPayUtils.vnp_TmnCode,
                vnpTxnRef, vnpTransactionDate, vnpCreateDate, vnpIpAddr, vnpOrderInfo
        );

        String vnpSecureHash = VNPayUtils.hmacSHA512(VNPayUtils.vnp_HashSecret, data);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", vnpRequestId);
        body.put("vnp_Version", vnpVersion);
        body.put("vnp_Command", vnpCommand);
        body.put("vnp_TmnCode", VNPayUtils.vnp_TmnCode);
        body.put("vnp_TxnRef", vnpTxnRef);
        body.put("vnp_OrderInfo", vnpOrderInfo);
        body.put("vnp_TransactionDate", vnpTransactionDate);
        body.put("vnp_CreateDate", vnpCreateDate);
        body.put("vnp_IpAddr", vnpIpAddr);
        body.put("vnp_SecureHash", vnpSecureHash);
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(body);
            log.info("VNPay querydr request: {}", json);

            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(VNPayUtils.vnp_apiUrl);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new org.apache.http.entity.StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpClient closeable = client; CloseableHttpResponse response = closeable.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                Map<String, String> vpnQueryResult = mapper.readValue(responseBody, Map.class);
                String hashSecret = (String) vpnQueryResult.get("vnp_SecureHash"); //need to check
                vpnQueryResult.remove("vnp_SecureHash");

                String calculatedHash = hashAllFields(vpnQueryResult);
                if (!hashSecret.equals(calculatedHash)) {
                    log.error("VNPay querydr hash mismatch: {}", vpnQueryResult);
                }

                if (!"00".equals(vpnQueryResult.get("vnp_ResponseCode"))) {
                    log.error("VNPay querydr failed: {}", vpnQueryResult);
                    return vpnQueryResult;
                }
                log.info("VNPay querydr response: {}", vpnQueryResult);
                return vpnQueryResult;
            }
        } catch (Exception ex) {
            log.error("VNPay querydr call failed: {}", ex.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", true);
            error.put("message", ex.getMessage());
            return error;
        }
    }

    @Override
    public RefundResponse refundPayment(String paymentId, HttpServletRequest httpServletRequest) {
        return new RefundResponse(
                null,
                VnPayTransactionStatus.SUCCESS.getCode().equals("00")
                    ? PaymentStatus.SUCCEEDED.getStatus()
                    : PaymentStatus.FAILED.getStatus()
        );
    }

    @Override
    public PaymentProvider getPaymentProviderName() {
        return PaymentProvider.VNPAY;
    }
}