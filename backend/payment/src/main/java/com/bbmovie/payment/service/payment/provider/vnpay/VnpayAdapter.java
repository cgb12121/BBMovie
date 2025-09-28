package com.bbmovie.payment.service.payment.provider.vnpay;

import com.bbmovie.payment.config.payment.VnpayProperties;
import com.bbmovie.payment.dto.PaymentCreatedEvent;
import com.bbmovie.payment.dto.PricingBreakdown;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.SubscriptionPlan;
import com.bbmovie.payment.entity.enums.BillingCycle;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.entity.enums.SupportedCurrency;
import com.bbmovie.payment.exception.PaymentCacheException;
import com.bbmovie.payment.exception.TransactionExpiredException;
import com.bbmovie.payment.exception.TransactionNotFoundException;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import com.bbmovie.payment.service.PaymentRecordService;
import com.bbmovie.payment.service.cache.RedisService;
import com.bbmovie.payment.service.nats.PaymentEventProducer;
import com.bbmovie.payment.service.payment.PricingService;
import com.bbmovie.payment.service.PaymentNormalizer;
import com.bbmovie.payment.service.SubscriptionPlanService;
import com.example.common.utils.IpAddressUtils;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.bbmovie.payment.service.payment.provider.vnpay.VnpayQueryParams.*;
import static com.bbmovie.payment.service.payment.provider.vnpay.VnpayTransactionStatus.SUCCESS;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.stringToJsonNode;
import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.toJsonString;
import static com.bbmovie.payment.utils.RandomUtil.getRandomNumber;

@Log4j2
@Service("vnpay")
public class VnpayAdapter implements PaymentProviderAdapter {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PricingService pricingService;
    private final PaymentRecordService paymentRecordService;
    private final VnpayProvidedFunction vnpayProvidedFunction;
    private final VnpayProperties properties;
    private final PaymentNormalizer normalizer;
    private final RedisService redisService;
    private final PaymentEventProducer paymentEventProducer;

    @Autowired
    public VnpayAdapter(
            PaymentTransactionRepository paymentTransactionRepository,
            SubscriptionPlanService subscriptionPlanService,
            VnpayProvidedFunction vnpayProvidedFunction,
            VnpayProperties properties,
            @Qualifier("vnpayNormalizer") PaymentNormalizer normalizer,
            PricingService pricingService,
            PaymentRecordService paymentRecordService,
            RedisService redisService,
            PaymentEventProducer paymentEventProducer
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.subscriptionPlanService = subscriptionPlanService;
        this.vnpayProvidedFunction = vnpayProvidedFunction;
        this.properties = properties;
        this.normalizer = normalizer;
        this.pricingService = pricingService;
        this.paymentRecordService = paymentRecordService;
        this.redisService = redisService;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional(noRollbackFor = PaymentCacheException.class)
    public PaymentCreationResponse createPaymentRequest(String userId, SubscriptionPaymentRequest request, HttpServletRequest hsr) {
        SubscriptionPlan plan = subscriptionPlanService.getById(UUID.fromString(request.subscriptionPlanId()));

        BillingCycle cycle = plan.getBillingCycle();
        if (cycle != BillingCycle.MONTHLY && cycle != BillingCycle.ANNUAL) {
            throw new IllegalArgumentException("Unexpected billing cycle.");
        }

        PricingBreakdown breakdown = pricingService.calculate(
                plan, cycle, SupportedCurrency.VND.unit(),
                userId, IpAddressUtils.getClientIp(hsr), request.voucherCode()
        );
        BigDecimal amount = breakdown.finalPrice();

        String amountInVnpayConvention = amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
        String vnpTxnRef = getRandomNumber(16);
        String paymentUrl = vnpayProvidedFunction.createOrder(
                hsr, amountInVnpayConvention , vnpTxnRef, "billpayment",
                properties.getTmnCode(), properties.getReturnUrl(), properties.getHashSecret(), properties.getPayUrl()
        );

        PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent(
                userId, plan, amount, SupportedCurrency.VND.unit(),
                PaymentProvider.VNPAY, vnpTxnRef, "VNPay payment for order: " + vnpTxnRef
        );

        PaymentTransaction transaction = paymentRecordService.createPendingTransaction(paymentCreatedEvent);
        redisService.cache(paymentCreatedEvent);

        return PaymentCreationResponse.builder()
                .provider(PaymentProvider.VNPAY)
                .serverTransactionId(String.valueOf(transaction.getId()))
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

        PaymentStatus.NormalizedPaymentStatus result = normalizer.normalize(responseCode);
        PaymentStatus normalizedCode = result.status();
        String message = result.message();

        boolean isValid = checkSum.equals(calculateChecksum) && SUCCESS.getCode().equals(responseCode);

        paymentTransactionRepository.findByProviderTransactionId(vnpTxnRef).ifPresent(tx -> {
            LocalDateTime now = LocalDateTime.now();
            if (tx.getExpiresAt() != null && now.isAfter(tx.getExpiresAt())) {
                throw new TransactionExpiredException("Payment expired");
            }

            // Only process if still pending; prevent replay from flipping canceled/refunded/succeeded
            if (tx.getStatus() != PaymentStatus.PENDING) {
                return;
            }

            tx.setPaymentMethod(paymentMethod);
            tx.setProviderTransactionId(vpnTransactionNo);
            tx.setResponseCode(responseCode);
            tx.setStatus(normalizedCode);
            tx.setResponseMessage(message);
            tx.setLastModifiedDate(now);

            paymentTransactionRepository.save(tx);
        });

        JsonNode providerData = stringToJsonNode(toJsonString(paymentData));
        paymentEventProducer.publishSubscriptionSuccessEvent();

        return PaymentVerificationResponse.builder()
                .isValid(isValid)
                .transactionId(vpnTransactionNo)
                .code(responseCode)
                .message(message)
                .clientResponse(providerData)
                .providerResponse(message)
                .build();
    }

    @Override
    public Object queryPayment(String userId, String paymentId) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        Map<String, String> body = vnpayProvidedFunction.createQueryOrder(txn, properties.getTmnCode(), properties.getHashSecret());
        log.info("Querying VNPay payment: {}", body);
        return vnpayProvidedFunction.executeRequest(body, properties.getApiUrl(), properties.getHashSecret());
    }

    @Override
    @Transactional
    public RefundResponse refundPayment(String userId, String paymentId, HttpServletRequest hsr) {
        PaymentTransaction txn = paymentTransactionRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(TransactionNotFoundException::new);
        Map<String, String> body = vnpayProvidedFunction.createRefundOrder(hsr, txn, properties.getTmnCode(), properties.getHashSecret());
        Map<String, String> result = vnpayProvidedFunction.executeRequest(body, properties.getApiUrl(), properties.getHashSecret());

       paymentEventProducer.publishSubscriptionCancelEvent();

        return new RefundResponse(
            result.get(VNPAY_TXN_REF_PARAM), 
            result.get(VNPAY_RESPONSE_CODE),
            result.get(VNPAY_MESSAGE),
            txn.getCurrency().getCurrencyCode(),
            new BigDecimal(result.get(VNPAY_AMOUNT_PARAM)).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
        );
    }
}