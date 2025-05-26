package com.example.bbmovie.service.payment.manager;

import com.example.bbmovie.service.payment.PaymentProviderType;
import com.example.bbmovie.service.payment.callback.PaymentCallbackHandler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CallbackStrategyManager {

    private final Map<PaymentProviderType, PaymentCallbackHandler> strategies;

    public CallbackStrategyManager(List<PaymentCallbackHandler> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(PaymentCallbackHandler::getProviderType, h -> h));
    }

    public PaymentCallbackHandler getHandler(PaymentProviderType type) {
        return strategies.get(type);
    }
}

