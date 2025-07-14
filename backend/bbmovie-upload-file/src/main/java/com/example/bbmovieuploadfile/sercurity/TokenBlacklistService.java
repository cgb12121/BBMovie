package com.example.bbmovieuploadfile.sercurity;

import reactor.core.publisher.Mono;

public interface TokenBlacklistService {
    Mono<Boolean> isBlacklisted(String jti);
}