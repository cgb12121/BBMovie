package com.bbmovie.gateway.security.anonymity.location;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * IpInfoLocationService is a service implementation of the LocationService
 * interface that retrieves location-related information based on an IP address.
 * This implementation uses the ipinfo.io API to fetch the country code associated
 * with the provided IP address.
 * <p>
 * The service relies on a token-based authentication mechanism to query the external
 * API. If the IP is invalid, the API request fails, or the request exceeds the rate
 * limit, the service will return an empty Optional.
 */
@Log4j2
@Service
public class IpInfoLocationService implements LocationService{

    @Value( "${ip.ipinfo.token}")
    private String ipInfoToken;

    /**
     * Retrieves the country code associated with a given IP address by querying an external IP information service.
     *
     * @param ip the IP address for which the country code is to be fetched; must be non-null and non-empty
     * @return an {@code Optional} containing the country code if retrieval is successful, or an empty {@code Optional}
     *         if the IP is invalid, the API request fails, or the request exceeds the rate limit
     */
    public Optional<String> getCountryCodeByIp(String ip) {
        IPinfo ipinfo = new IPinfo.Builder()
                .setToken(ipInfoToken)
                .build();
        try {
            IPResponse response = ipinfo.lookupIP(ip);
            return Optional.ofNullable(response.getCountryCode());
        } catch (RateLimitedException e) {
            log.error("Api rate limit exceeded: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
