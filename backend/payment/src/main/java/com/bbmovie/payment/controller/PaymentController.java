package com.bbmovie.payment.controller;

import com.bbmovie.payment.dto.*;
import com.bbmovie.payment.dto.request.PaymentRequest;
import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.bbmovie.payment.dto.request.RefundRequest;
import com.bbmovie.payment.dto.response.PaymentCreationResponse;
import com.bbmovie.payment.dto.response.PaymentVerificationResponse;
import com.bbmovie.payment.dto.response.RefundResponse;
import com.bbmovie.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.bbmovie.payment.utils.PaymentProviderPayloadUtil.*;

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
    public ResponseEntity<ApiResponse<PaymentCreationResponse>> initiatePayment(
            @SuppressWarnings("unused") @RequestHeader("Authorization") String jwtToken, // This Will be used to pass the user id
            @RequestBody PaymentRequest request, HttpServletRequest httpServletRequest
    ) {
        try {
            PaymentCreationResponse response = paymentService.createPayment(request.getProvider(), request, httpServletRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Unable to process payment"));
        }
    }

    /**
     *  <b>WARN</b>
     *  <p> This endpoint uses for testing </p>
     *  <p> In prod should redirect to FE instead of this endpoint, then FE makes post-request</p>
     */
    @GetMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleProviderCallbackGet(
            @RequestParam Map<String, String> params, HttpServletRequest request, @PathVariable String provider
    ) {
        log.info("\nReceived payment callback from provider: {}.\n {}", provider,params);
        PaymentVerificationResponse verification = paymentService.handleCallback(provider, params, request);
        if (verification.isValid()) {
            return ResponseEntity.ok(ApiResponse.success(verification));
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
    }

    /*
        Zalopay expect response from the server:
            for v1:
            {
              "returncode": "[...]",
              "returnmessage": "[...]"
            }

            for v2:
            {
              "return_code": "[...]",
              "return_message": "[...]"
            }
     */
    @PostMapping("/zalopay/callback")
    public ResponseEntity<Map<String, String>> handleProviderCallbackPost(
            @RequestBody Map<String, String> body, HttpServletRequest request
    ) {
        log.info("\nReceived payment callback from ZALOPAY.\n {}", body);
        PaymentVerificationResponse verification = paymentService.handleCallback("zalopay", body, request);
        @SuppressWarnings("unchecked") // The data has been made sure to match
        Map<String, String> providerPayload = stringToJson(verification.getResponseToProviderStringJson().toString(), Map.class);
        if (verification.isValid()) {
            return ResponseEntity.ok(providerPayload);
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(providerPayload);
    }

//    @PostMapping("/paypal/webhook")
//    public ResponseEntity<Void> handlePayPalWebhook(@RequestBody String payload,
//                                                    @RequestHeader Map<String, String> headers,
//                                                    HttpServletRequest request) {
//        log.info("PayPal webhook received: {}", payload);
//        try {
//            CallbackRequestContext ctx = CallbackRequestContext.builder()
//                    .httpServletRequest(request)
//                    .headers(headers)
//                    .rawBody(payload)
//                    .httpMethod("POST")
//                    .contentType(request.getContentType())
//                    .build();
//            PaymentVerificationResponse verification = paymentService.handleWebhook("paypal", ctx);
//            return verification.isValid() ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        } catch (UnsupportedOperationException ex) {
//            log.warn("Webhook not supported by provider paypal: {}", ex.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
//        }
//    }
//
//    @PostMapping("/paypal/ipn")
//    public ResponseEntity<Void> handlePayPalIPN(@RequestParam Map<String, String> params, HttpServletRequest request) {
//        log.info("PayPal IPN received: {}", params);
//        CallbackRequestContext ctx = CallbackRequestContext.builder()
//                .httpServletRequest(request)
//                .formParams(params)
//                .httpMethod("POST")
//                .contentType(request.getContentType())
//                .build();
//        try {
//            PaymentVerificationResponse verification = paymentService.handleIpn("paypal", ctx);
//            return verification.isValid() ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        } catch (UnsupportedOperationException ex) {
//            log.warn("IPN not supported by provider paypal: {}", ex.getMessage());
//            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
//        }
//    }

    @PostMapping("/{provider}/ipn")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleGenericIpn(
            @PathVariable String provider,
            @RequestParam Map<String, String> params,
            HttpServletRequest request
    ) {
        CallbackRequestContext ctx = CallbackRequestContext.builder()
                .httpServletRequest(request)
                .formParams(params)
                .httpMethod("POST")
                .contentType(request.getContentType())
                .build();
        try {
            PaymentVerificationResponse verification = paymentService.handleIpn(provider, ctx);
            if (verification.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(verification));
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (UnsupportedOperationException ex) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ApiResponse.error("IPN not supported by provider"));
        }
    }

    @PostMapping("/{provider}/webhook")
    public ResponseEntity<ApiResponse<PaymentVerificationResponse>> handleGenericWebhook(
            @PathVariable String provider,
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers,
            HttpServletRequest request
    ) {
        CallbackRequestContext ctx = CallbackRequestContext.builder()
                .httpServletRequest(request)
                .headers(headers)
                .rawBody(payload)
                .httpMethod("POST")
                .contentType(request.getContentType())
                .build();
        try {
            PaymentVerificationResponse verification = paymentService.handleWebhook(provider, ctx);
            if (verification.isValid()) {
                return ResponseEntity.ok(ApiResponse.success(verification));
            }
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiResponse.fail(verification));
        } catch (UnsupportedOperationException ex) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ApiResponse.error("Webhook not supported by provider"));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> refundPayment(@RequestBody RefundRequest request, HttpServletRequest httpServletRequest) {
        RefundResponse response = paymentService.refundPayment(String.valueOf(request.getProvider()), String.valueOf(request.getPaymentId()), httpServletRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/query")
    public ResponseEntity<ApiResponse<Object>> queryPaymentFromProvider(@RequestParam String id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.queryPayment(id, request)));
    }
}
