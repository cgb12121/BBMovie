package com.example.bbmovie.security.anonymity.vpnapi;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Service
public class VpnApiService {

    @Value("${ip.vpnapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean detectAnonymity(String ip) {
        String url = "https://vpnapi.io/api/" + ip + "?key=" + apiKey;
        try {
            VpnApiResponse response = restTemplate.getForObject(url, VpnApiResponse.class);
            log.info("VPN API response for IP {}: {}", ip, response);
            return response != null && response.getSecurity() != null && isAnonymity(response);
        } catch (Exception e) {
            log.error("Failed to get VPN API response for IP {}: {}", ip, e.getMessage());
            return false;
        }
    }

    private boolean isAnonymity(VpnApiResponse response) {
        Security security = response.getSecurity();
        return security.isVpn() || security.isProxy() || security.isTor() || security.isRelay();
    }
}
