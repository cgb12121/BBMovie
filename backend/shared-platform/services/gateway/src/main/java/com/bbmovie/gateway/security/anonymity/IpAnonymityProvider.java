package com.bbmovie.gateway.security.anonymity;

import reactor.core.publisher.Mono;

public interface IpAnonymityProvider {
    Mono<Boolean> isAnonymity(String ip);
    String getName();
}
