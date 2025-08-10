package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.*;
import com.bbmovie.payment.service.PaymentService;
import jakarta.annotation.security.DenyAll;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

    // Vnpay
    @GetMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleProviderCallbackGet
            (@RequestParam Map<String, String> params, HttpServletRequest request,
             @PathVariable String provider) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(provider, params, request);
            if (verification.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(verification));
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (Exception e) {
            log.fatal("[FATAL][GetMapping] Error processing {} callback: {}", provider, e.getMessage());
            String errorMessage = createErrorMessage(provider);
            return ResponseEntity.internalServerError().body(ApiResponse.error(errorMessage));
        }
    }

    // Zalopay
    @PostMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<PaymentVerification>> handleProviderCallbackPost(
        @RequestBody Map<String, String> body, HttpServletRequest request, @PathVariable String provider) {
        try {
            PaymentVerification verification = paymentService.verifyPayment(provider, body, request);
            if (verification.isValid()) return ResponseEntity.ok(ApiResponse.success(verification));
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (Exception e) {
            log.fatal("[FATAL][PostMapping] Error processing {} callback: {}", provider, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(createErrorMessage(provider)));
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

    @DenyAll
    @RequestMapping(
            value = "/{provider}/callback/combined-not-test",
            method = {
                    RequestMethod.GET,
                    RequestMethod.POST
            }
    )
    public ResponseEntity<ApiResponse<PaymentVerification>> handleProviderCallback(
            @RequestParam(required = false) Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request,
            @PathVariable String provider) {
        log.info("Received callback for provider {}: queryParams={}, body={}", provider, queryParams, body);
        try {
            Map<String, String> params = new HashMap<>();

            if ("vnpay".equalsIgnoreCase(provider)) {
                if (queryParams == null || queryParams.isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("No query parameters provided for VNPay"));
                }
                params.putAll(queryParams);
            } else if ("zalopay".equalsIgnoreCase(provider)) {
                if (body == null || body.isEmpty()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("No body provided for ZaloPay"));
                }
                String data = body.get("data");
                String mac = body.get("mac");
                if (data == null || mac == null || data.isBlank() || mac.isBlank()) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Missing or invalid data/mac for ZaloPay callback"));
                }
                params.put("data", data);
                params.put("mac", mac);
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Unsupported provider: " + provider));
            }

            PaymentVerification verification = paymentService.verifyPayment(provider, params, request);
            if (verification.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(verification));
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (Exception e) {
            log.fatal("[FATAL] Error processing callback for {}: {}", provider, e.toString());
            return ResponseEntity.internalServerError().body(ApiResponse.error(createErrorMessage(provider)));
        }
    }

    private static String createErrorMessage(String provider) {
        return """
            Unable to verify payment from %1$s.
            Please contact the admin or %1$s hotline for further assistance.
        """.formatted(provider.toUpperCase());
    }
}
