package com.bbmovie.payment.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Service
@SuppressWarnings("all")
public class CurrencyConversionService {

    private final RestTemplate restTemplate = new RestTemplate();

    public BigDecimal convert(String from, String to, BigDecimal amount) {
        String url = String.format(
                "https://api.exchangerate.host/convert?from=%s&to=%s&amount=%s",
                from,
                to,
                amount.toPlainString()
        );

        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map body = response.getBody();
            Object result = body.get("result");
            if (result != null) {
                return new BigDecimal(result.toString());
            }
        }
        throw new RuntimeException("Conversion failed");
    }
}