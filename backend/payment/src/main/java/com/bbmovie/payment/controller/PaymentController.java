package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.*;
import com.bbmovie.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment
            (@RequestHeader("Authorization") String jwtToken, // This Will be used to pass the user id
             @RequestBody PaymentRequestDto request, HttpServletRequest httpServletRequest) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setPaymentMethodId(request.getProvider());
            paymentRequest.setAmount(request.getAmount());
            paymentRequest.setCurrency(request.getCurrency());
            paymentRequest.setUserId(request.getUserId());
            paymentRequest.setOrderId(request.getOrderId());
            log.info("Initiating payment for order: {}", request.toString());

            PaymentResponse response = paymentService.processPayment(
                    String.valueOf(request.getProvider()), paymentRequest, httpServletRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to process payment"));
        }
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleVNPayCallback
            (@RequestParam Map<String, String> params, HttpServletRequest request,
             @PathVariable String provider) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(provider, params, request);
            if (verification.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(verification));
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (Exception e) {
            log.fatal("[FATAL] Error processing VNPay callback: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(
                    """
                    Unable to verify payment from VNPay.
                    Please contact the admin or Vnpay hotline for further assistance.
                    """
            ));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refundPayment
            (@RequestBody RefundRequestDto request, HttpServletRequest httpServletRequest) {
        RefundResponse response = paymentService.refundPayment(String.valueOf(request.getProvider()), String.valueOf(request.getPaymentId()), httpServletRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<Object>> queryPaymentFromProvider(@RequestParam String id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.queryPayment(id, request)));
    }
}
