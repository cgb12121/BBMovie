package com.bbmovie.fileservice.sercurity.jose;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;

@Log4j2
@Service
public class TokenRevokeEventListener {

    private final TokenBlacklistService tokenBlacklistService;
    private final Connection nats;

    @Autowired
    public TokenRevokeEventListener(TokenBlacklistService tokenBlacklistService, Connection nats) {
        this.tokenBlacklistService = tokenBlacklistService;
        this.nats = nats;
        Dispatcher dispatcher = nats.createDispatcher(msg -> {
            String jti = new String(msg.getData(), java.nio.charset.StandardCharsets.UTF_8);
            log.info("Received token revoke event: {}", jti);
            tokenBlacklistService.addTokenToBlacklist(jti)
                    .doOnError(e -> log.error("Error blacklisting token: {}", e.getMessage()))
                    .subscribe();
        });
        dispatcher.subscribe("auth.token.revoke");
    }
}