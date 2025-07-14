package com.bbmovie.fileservice.sercurity.jose;

import reactor.core.publisher.Mono;

public interface TokenBlacklistService {
    Mono<Boolean> isBlacklisted(String jti);
    Mono<Void> addTokenToBlacklist(String jti);
}