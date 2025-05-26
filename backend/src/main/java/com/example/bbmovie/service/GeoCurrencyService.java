package com.example.bbmovie.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class GeoCurrencyService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<String> getCurrencyByIp(String ip) {
        String url = "https://ipwho.is/" + ip;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map body = response.getBody();
            if (Boolean.TRUE.equals(body.get("success"))) {
                return Optional.ofNullable((String) body.get("currency")).map(c -> ((Map)c).get("code").toString());
            }
        }
        return Optional.empty();
    }
}