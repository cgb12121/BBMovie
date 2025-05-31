package com.example.bbmovie.service.location;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class IpInfoLocationService implements LocationService {

    @Value( "${ipinfo.token}")
    private String ipInfoToken;

    @Override
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
