package com.example.bbmovie.service.payment.manager;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.dto.PaymentRequest;
import com.example.bbmovie.service.payment.dto.PaymentResponse;
import com.example.bbmovie.service.payment.strategy.PaymentStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentStrategyManager {

    private final Map<PaymentProviderType, PaymentStrategy> strategies;

    @Autowired
    public PaymentStrategyManager(List<PaymentStrategy> strategyList) {
        strategies = strategyList.stream()
            .collect(Collectors.toMap(PaymentStrategy::getProviderType, s -> s));
    }

    public PaymentResponse createPayment(PaymentRequest request, HttpServletRequest httpServletRequest) {
        PaymentStrategy strategy = strategies.get(request.getProvider());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + request.getProvider());
        }
        return strategy.createPayment(request, httpServletRequest);
    }
}
