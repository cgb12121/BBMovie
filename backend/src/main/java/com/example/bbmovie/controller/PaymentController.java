package com.example.bbmovie.controller;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.manager.CallbackStrategyManager;
import com.example.bbmovie.service.payment.manager.PaymentStrategyManager;
import com.example.bbmovie.service.payment.callback.PaymentCallbackHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentStrategyManager paymentManager;
    private final CallbackStrategyManager callbackManager;

    @PostMapping("/create")
    public void createPayment(@RequestBody PaymentRequest request, HttpServletRequest httpServletRequest, HttpServletResponse response) {
        PaymentResponse paymentResponse = paymentManager.createPayment(request, httpServletRequest);
        try {
            response.sendRedirect(paymentResponse.getPaymentUrl());
        } catch (IOException e) {
            log.error("Failed to redirect to payment page", e);
        }
    }

    @PostMapping("/callback/{provider}")
    public ResponseEntity<?> handleCallback(
            @PathVariable PaymentProviderType provider,
            @RequestBody(required = false) String body,
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request
    ) {
        PaymentCallbackHandler handler = callbackManager.getHandler(provider);
        if (handler == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No handler found");
        }
        handler.handleCallback(body, queryParams, request);
        return ResponseEntity.ok("Callback processed");
    }

    @GetMapping("/return/{provider}")
    public ResponseEntity<?> handleReturn(
            @PathVariable PaymentProviderType provider,
            @RequestParam Map<String, String> queryParams
    ) {
        PaymentCallbackHandler handler = callbackManager.getHandler(provider);
        if (handler == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No return handler found");
        }
        return handler.handleReturn(queryParams);
    }
}

