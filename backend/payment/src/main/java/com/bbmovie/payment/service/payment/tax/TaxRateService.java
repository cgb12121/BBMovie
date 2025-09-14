package com.bbmovie.payment.service.payment.tax;

import com.bbmovie.payment.dto.request.TaxRateRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class TaxRateService {

    private final RestTemplate restTemplate;

    @Value("${api.url.tax-rate}")
    private String baseUrl;

    @Value("${api.key.tax-rate}")
    private String apiKey;

    @Autowired
    public TaxRateService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public BigDecimal taxRate(TaxRateRequest request) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/tax_rates");

        // add only non-null params
        if (request.getZip() != null) uriBuilder.queryParam("zip", request.getZip());
        if (request.getUseClientIp() != null) uriBuilder.queryParam("use_client_ip", request.getUseClientIp());
        if (request.getStreet() != null) uriBuilder.queryParam("street", request.getStreet());
        if (request.getState() != null) uriBuilder.queryParam("state", request.getState());
        if (request.getIpAddress() != null) uriBuilder.queryParam("ip_address", request.getIpAddress());
        if (request.getCountry() != null) uriBuilder.queryParam("country", request.getCountry());
        if (request.getCity() != null) uriBuilder.queryParam("city", request.getCity());

        URI uri = uriBuilder.build().toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class);

        Assert.notNull(response.getBody(), "Response body is null");

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        log.info("Tax rates response: {}", body);

        List<BigDecimal> candidates = new ArrayList<>();
        extractRates(body, candidates);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No tax rate found in API response: " + body);
        }

        return candidates.stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

    }

    @SuppressWarnings("all")
    private void extractRates(Object node, List<BigDecimal> collector) {
        if (node == null) return;

        if (node instanceof Map<?, ?> mapNode) {
            for (Object value : mapNode.values()) {
                extractRates(value, collector);
            }
        } else if (node instanceof List<?> listNode) {
            for (Object value : listNode) {
                extractRates(value, collector);
            }
        } else if (node instanceof Number number) {
            BigDecimal val = new BigDecimal(number.toString());
            if (val.compareTo(BigDecimal.ZERO) >= 0 && val.compareTo(BigDecimal.valueOf(100)) <= 0) {
                collector.add(val);
            }
        } else if (node instanceof String str) {
            try {
                BigDecimal val = new BigDecimal(str);
                if (val.compareTo(BigDecimal.ZERO) >= 0 && val.compareTo(BigDecimal.valueOf(100)) <= 0) {
                    collector.add(val);
                }
            } catch (NumberFormatException ignored) {
                log.error("Invalid number format: {}", ignored);
            }
        }
    }
}