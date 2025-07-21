package com.bbmovie.auth.security.anonymity.ipapi;

import com.bbmovie.auth.security.anonymity.IpAnonymityProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class IpApiService implements IpAnonymityProvider {

    @Value("${ip.ipapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isAnonymity(String ip) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.ipapi.is/?q=").append(ip);
        if (apiKey != null) {
            sb.append("&key=").append(apiKey);
        }
        String url = sb.toString();
        try {
            IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);
            if (response != null) {
                log.info("IP API response for IP {}: {}", ip, response.toString());
                return checkAnonymity(response);
            }
        } catch (Exception e) {
            log.error("Failed to get IP API response for IP {}: {}", ip, e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public String getName() {
        return "ipapi.is";
    }

    private boolean checkAnonymity(IpApiResponse response) {
        return response.is_abuser() || response.is_vpn() || response.is_proxy() || response.is_tor() || response.is_crawler();
    }
}
