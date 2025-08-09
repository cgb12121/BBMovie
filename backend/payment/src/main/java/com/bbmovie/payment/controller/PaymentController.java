package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.*;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @RequestBody PaymentRequestDto request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setPaymentMethodId(request.getProvider());
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setCurrency(request.getCurrency());
            paymentRequest.setUserId(request.getUserId());
            paymentRequest.setOrderId(request.getOrderId());
            log.info("Initiating payment for order: {}", request.toString());

            PaymentResponse response = paymentService.processPayment(
                    String.valueOf(request.getProvider()), paymentRequest, httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Unable to process payment")
            );
        }
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleMomoIpn(
            @RequestBody Map<String, String> paymentData,
            HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(
                    "momoProvider", paymentData, httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(verification));
        } catch (Exception e) {
            log.error("Error processing MoMo IPN: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(new PaymentVerification(false, null).toString())
            );
        }
    }

    @PostMapping("/zalopay/callback")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleZaloPayCallback(
            @RequestParam Map<String, String> params,
            HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(
                    "zalopayProvider", params, httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(verification));
        } catch (Exception e) {
            log.error("Error processing ZaloPay callback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(new PaymentVerification(false, null).toString())
            );
        }
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleVNPayCallback(
            @RequestParam Map<String, String> params,
            HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(
                    "vnpayProvider", params, httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(verification));
        } catch (Exception e) {
            log.error("Error processing VNPay callback: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(new PaymentVerification(false, null).toString())
            );
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleWebhook(
            @RequestHeader("X-Payment-Provider") String provider,
            @RequestBody Map<String, String> paymentData,
            HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(provider, paymentData, httpServletRequest);
            return ResponseEntity.ok(ApiResponse.success(verification));
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(new PaymentVerification(false, null).toString())
            );
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refundPayment(
            @RequestBody RefundRequestDto request,
            HttpServletRequest httpServletRequest
    ) {
        try {
            RefundResponse response = paymentService.refundPayment(
                    String.valueOf(request.getProvider()),
                    String.valueOf(request.getPaymentId()),
                    httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error processing refund: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(new RefundResponse(null, PaymentStatus.FAILED.getStatus()).toString())
            );
        }
    }
}
