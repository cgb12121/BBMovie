package com.bbmovie.fileservice.sercurity;

import reactor.core.publisher.Mono;

public interface TokenBlacklistService {
    Mono<Boolean> isBlacklisted(String jti);
}