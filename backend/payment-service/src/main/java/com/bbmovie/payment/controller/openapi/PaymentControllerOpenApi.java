package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.RefundRequest;
import com.bbmovie.payment.dto.request.SubscriptionPaymentRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@SuppressWarnings("unused")
@Tag(name = "Payments", description = "Payment initiation, callback, verification and refund APIs")
public interface PaymentControllerOpenApi {
    @Operation(summary = "Initiate payment", description = "Create a payment transaction", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<ApiResponse<PaymentCreationResponse>> initiatePayment(
            @NotBlank @RequestHeader("Authorization") String jwtToken,
            @RequestBody SubscriptionPaymentRequest request,
            HttpServletRequest hsr
    );

    @Operation(summary = "Provider callback (GET)", description = "Handle provider callback for testing")
    ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleProviderCallbackGet(
            @RequestParam Map<String, String> params,
            HttpServletRequest request,
            @PathVariable String provider
    );

    @Operation(summary = "ZaloPay callback (POST)", description = "Handle ZaloPay callback payload")
    ResponseEntity<Map<String, String>> handleProviderCallbackPost(
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    );

    @Operation(summary = "Provider IPN handler", description = "Handle generic provider IPN notifications")
    ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleGenericIpn(
            @PathVariable String provider,
            @RequestParam Map<String, String> params,
            HttpServletRequest request
    );

    @Operation(summary = "Provider webhook handler", description = "Handle generic provider webhooks")
    ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleGenericWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    );

    @Operation(summary = "Refund payment", description = "Refund a completed payment", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<ApiResponse<RefundResponse>> refundPayment(
            @NotBlank @RequestHeader("Authorization") String jwtToken,
            @RequestBody RefundRequest request,
            HttpServletRequest hsr
    );

    @Operation(summary = "Query payment from provider", description = "Query payment transaction status by provider ID", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<ApiResponse<Object>> queryPaymentFromProvider(
            @NotBlank @RequestHeader("Authorization") String jwtToken,
            @RequestParam String id
    );
}

