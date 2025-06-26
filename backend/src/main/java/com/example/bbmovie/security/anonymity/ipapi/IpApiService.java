package com.example.bbmovie.security.anonymity.ipapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IpApi {

    @Value("${ip.ipapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isAnonymity(String ip) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.ipapi.is/?q=").append(ip);
        if (apiKey != null) {
            sb.append("&key=").append(apiKey);
        }
        String url = sb.toString();
        IpApiResponse response = restTemplate.getForObject(url, IpApiResponse.class);

        return false;
    }

    private boolean isAnonymity(IpApiResponse response) {
        return response.is_abuser || response.is_vpn || response.is_proxy || response.is_tor || response.is_crawler;
    }
}
