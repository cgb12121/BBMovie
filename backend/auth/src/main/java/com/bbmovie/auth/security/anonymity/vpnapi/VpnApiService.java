package com.bbmovie.auth.security.anonymity.vpnapi;

import com.bbmovie.auth.security.anonymity.IpAnonymityProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Log4j2
@Component("vpnapiProvider")
public class VpnApiService implements IpAnonymityProvider {

    @Value("${ip.vpnapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isAnonymity(String ip) {
        String url = "https://vpnapi.io/api/" + ip + "?key=" + apiKey;
        try {
            VpnApiResponse response = restTemplate.getForObject(url, VpnApiResponse.class);
            log.info("VPN API response for IP {}: {}", ip, response);
            return response != null && response.getSecurity() != null && checkAnonymity(response);
        } catch (Exception e) {
            log.error("Failed to get VPN API response for IP {}: {}", ip, e.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return "vpnapi.io";
    }

    private boolean checkAnonymity(VpnApiResponse response) {
        Security security = response.getSecurity();
        return security.isVpn() || security.isProxy() || security.isTor() || security.isRelay();
    }
}
