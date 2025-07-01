package com.example.bbmovie.service.location;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

@Service
@Log4j2(topic = "IpWhoLocationService")
public class IpWhoLocationService implements LocationService{

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @SuppressWarnings("all")
    public Optional<String> getCountryCodeByIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            log.error("IP address is null or empty");
            return Optional.empty();
        }

        String url = UriComponentsBuilder.fromUriString("https://ipwho.is/")
                .pathSegment(ip)
                .toUriString();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            log.info("Response from API: {}", response);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map body = response.getBody();
                if (Boolean.TRUE.equals(body.get("success"))) {
                    String countryCode = (String) body.get("country_code");
                    return Optional.ofNullable(countryCode);
                } else {
                    log.error("API returned success: false, response body: {}", body);
                }
            } else {
                log.error("HTTP error, status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling API: {}", e.getMessage());
        }

        return Optional.empty();
    }
}