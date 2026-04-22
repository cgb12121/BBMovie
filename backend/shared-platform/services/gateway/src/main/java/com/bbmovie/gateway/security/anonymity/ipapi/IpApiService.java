package com.bbmovie.gateway.security.anonymity.ipapi;

import com.bbmovie.gateway.security.anonymity.IpAnonymityProvider;
import com.bbmovie.gateway.security.anonymity.ipapi.response.IpApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Log4j2
@Service
public class IpApiService implements IpAnonymityProvider {

    @Value("${ip.ipapi.api-key}")
    private String apiKey;

    private final WebClient webClient;

    public IpApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.ipapi.is/")
                .build();
    }

    @Override
    public Mono<Boolean> isAnonymity(String ip) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.queryParam("q", ip)
                        .queryParam("key", apiKey)
                        .build()
                )
                .retrieve()
                .bodyToMono(IpApiResponse.class)
                .map(this::checkAnonymity)
                .doOnSuccess(response -> log.info("IP API response for IP {}: {}", ip, response))
                .doOnError(e -> log.error("Failed to get IP API response for IP {}: {}", ip, e.getMessage()))
                .onErrorReturn(false);
    }

    @Override
    public String getName() {
        return "ipapi.is";
    }

    private boolean checkAnonymity(IpApiResponse response) {
        return response.isAbuser() || response.isVpn() || response.isProxy() || response.isTor() || response.isCrawler();
    }
}
