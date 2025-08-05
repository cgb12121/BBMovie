package com.bbmovie.auth.security.anonymity.vpnapi;

import com.bbmovie.auth.security.anonymity.IpAnonymityProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * The {@code VpnApiService} class implements the {@code IpAnonymityProvider} interface
 * and provides functionality to determine whether an IP address exhibits anonymity
 * characteristics, such as being associated with a VPN, proxy, Tor node, or relay.
 * <p>
 * This service uses the vpnapi.io REST API to determine the anonymity status of
 * the provided IP address. The API key required for authentication is injected
 * via a configuration property.
 * <p>
 * Key operations provided by this class include:
 * <p>
 * - Checking if an IP address is anonymous by interacting with the vpnapi.io API
 *   and analyzing the response.
 * - Retrieving the name of the provider ("vpnapi.io").
 * <p>
 * Logging is used to track the API responses and any issues encountered during
 * the process.
 */
@Log4j2
@Component("vpnapiProvider")
public class VpnApiService implements IpAnonymityProvider {

    @Value("${ip.vpnapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Checks if the given IP address exhibits anonymity characteristics, such as being
     * associated with a VPN, proxy, Tor node, or other anonymity-related mechanisms.
     *
     * @param ip the IP address to be checked for anonymity
     * @return true if the IP address is identified as anonymous, false otherwise
     */
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

    /**
     * Retrieves the name of the IP anonymity provider implementation.
     *
     * @return the name of the IP anonymity provider, which in this case is "vpnapi.io"
     */
    @Override
    public String getName() {
        return "vpnapi.io";
    }

    /**
     * Checks whether the given {@code VpnApiResponse} contains any indicators of
     * anonymity such as being associated with a VPN, proxy, Tor network, or relay.
     *
     * @param response the {@code VpnApiResponse} object containing security details to be evaluated
     * @return {@code true} if the response indicates anonymity through VPN, proxy, Tor, or relay; {@code false} otherwise
     */
    private boolean checkAnonymity(VpnApiResponse response) {
        Security security = response.getSecurity();
        return security.isVpn() || security.isProxy() || security.isTor() || security.isRelay();
    }
}
