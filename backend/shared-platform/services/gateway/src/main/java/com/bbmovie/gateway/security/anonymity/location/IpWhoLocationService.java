package com.bbmovie.gateway.security.anonymity.location;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

/**
 * IpWhoLocationService is a service implementation of the LocationService interface that retrieves location-related
 * information based on an IP address. This implementation uses the ipwho.is API to fetch the country code
 * corresponding to the provided IP address.
 * <p>
 * The service queries the external API using an HTTP GET request and processes the response to extract the
 * country code. If the IP is invalid, the API request fails, or the response indicates an error, the service
 * will return an empty Optional.
 */
@Log4j2
@Service
public class IpWhoLocationService implements LocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Retrieves the country code associated with a given IP address by querying an external IP location service.
     *
     * @param ip the IP address for which the country code is to be fetched; must be non-null and non-empty
     * @return an {@code Optional} containing the country code if successful, or an empty {@code Optional} if
     *         the IP is invalid, the API request fails, or the response indicates failure
     */
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