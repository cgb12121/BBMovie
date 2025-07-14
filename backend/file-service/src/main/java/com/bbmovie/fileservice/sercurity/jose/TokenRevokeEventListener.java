package com.bbmovie.fileservice.sercurity.jose;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class TokenRevokeEventListener {

    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    public TokenRevokeEventListener(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @KafkaListener(topics = "token-revoke", groupId = "file-service-group")
    public void handleUploadEvent(String jti, Acknowledgment acknowledgment) {
        log.info("Received token revoke event: {}", jti);
        tokenBlacklistService.addTokenToBlacklist(jti)
                .doOnSuccess(v -> acknowledgment.acknowledge())
                .doOnError(e -> log.error("Error blacklisting token: {}", e.getMessage()))
                .subscribe();
    }
}
