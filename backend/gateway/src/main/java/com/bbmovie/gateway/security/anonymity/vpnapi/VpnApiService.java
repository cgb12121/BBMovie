package com.bbmovie.gateway.security.anonymity.vpnapi;

import com.bbmovie.gateway.security.anonymity.IpAnonymityProvider;
import com.bbmovie.gateway.security.anonymity.vpnapi.response.Security;
import com.bbmovie.gateway.security.anonymity.vpnapi.response.VpnApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Log4j2
@Component("vpnapiProvider")
public class VpnApiService implements IpAnonymityProvider {

    @Value("${ip.vpnapi.api-key}")
    private String apiKey;

    private final WebClient webClient;

    public VpnApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://vpnapi.io/api/")
                .build();
    }

    @Override
    public Mono<Boolean> isAnonymity(String ip) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(ip)
                        .queryParam("key", apiKey)
                        .build()
                )
                .retrieve()
                .bodyToMono(VpnApiResponse.class)
                .map(response -> response != null
                        && response.getSecurity() != null
                        && checkAnonymity(response)
                )
                .doOnSuccess(isAnonymous -> log.info("VPN API check for IP {}: {}", ip, isAnonymous))
                .doOnError(e -> log.error("Failed to get VPN API response for IP {}: {}", ip, e.getMessage()))
                .onErrorReturn(false);
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