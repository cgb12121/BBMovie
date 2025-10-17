package com.bbmovie.auth.security.anonymity.ipapi;

import com.bbmovie.auth.security.anonymity.IpAnonymityProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * The {@code IpApiService} class is a service implementation of the {@code IpAnonymityProvider}
 * interface. It integrates with the ipapi.is API to determine the anonymity characteristics
 * of an IP address. The service checks for indicators such as VPN usage, proxy usage, Tor network
 * activity, crawling behavior, or abusive activity associated with the given IP.
 * <p>
 * This service uses a {@code RestTemplate} to communicate with the ipapi.is API endpoint and
 * processes the API response to determine anonymity-related attributes. Additionally, the class
 * provides metadata about its implementation via the {@code getName()} method.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
@Log4j2
@Service
public class IpApiService implements IpAnonymityProvider {

    @Value("${ip.ipapi.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Determines whether the given IP address is associated with anonymity-related activity.
     * This includes activities such as using a VPN, proxy, Tor network, or being identified
     * as an abuser or crawler.
     *
     * @param ip the IP address to be checked for anonymity indicators
     * @return true if the IP is associated with anonymity-related activity, false otherwise
     */
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

    /**
     * Retrieves the name of the IP anonymity provider implementation.
     *
     * @return the name of the IP anonymity provider as a string, which is "ipapi.is".
     */
    @Override
    public String getName() {
        return "ipapi.is";
    }

    /**
     * Determines whether the given IP's response data suggests anonymity based on specific attributes.
     *
     * @param response an instance of IpApiResponse containing details about an IP's metadata
     * @return true if the IP is categorized as an abuser, VPN user, proxy user, Tor user, or crawler; false otherwise
     */
    private boolean checkAnonymity(IpApiResponse response) {
        return response.isAbuser() || response.isVpn() || response.isProxy() || response.isTor() || response.isCrawler();
    }
}
