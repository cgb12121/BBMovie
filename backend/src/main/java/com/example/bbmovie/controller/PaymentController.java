package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.request.PaymentRequestDto;
import com.example.bbmovie.dto.request.RefundRequestDto;
import com.example.bbmovie.service.payment.PaymentService;
import com.example.bbmovie.service.payment.PaymentStatus;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.dto.PaymentVerification;
import com.example.bbmovie.service.payment.dto.RefundResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setCurrency(request.getCurrency());
            paymentRequest.setUserId(request.getUserId());
            paymentRequest.setOrderId(request.getOrderId());
            PaymentResponse response = paymentService.processPayment(request.getProvider(), paymentRequest, httpServletRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(String.valueOf(new PaymentResponse(null, PaymentStatus.FAILED, e.getMessage())))
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
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(String.valueOf(new PaymentVerification(false, null)))
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
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(String.valueOf(new PaymentVerification(false, null)))
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
                    request.getProvider(), request.getPaymentId(), httpServletRequest
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(String.valueOf(new RefundResponse(null, PaymentStatus.FAILED.getStatus())))
            );
        }
    }
}