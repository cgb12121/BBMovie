package com.example.bbmovie.service.payment.callback;

import com.example.bbmovie.service.payment.PaymentProviderType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentCallbackHandler {
    PaymentProviderType getProviderType();
    void handleCallback(String body, Map<String, String> queryParams, HttpServletRequest request);
    ResponseEntity<?> handleReturn(Map<String, String> queryParams);
}
