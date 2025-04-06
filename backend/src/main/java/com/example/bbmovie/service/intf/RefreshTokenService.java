package com.example.bbmovie.service.intf;

import org.springframework.stereotype.Service;

@Service
public interface RefreshTokenService {
    String createRefreshToken(String username);

    String getUsernameFromRefreshToken(String tokenId);

    void deleteRefreshToken(String tokenId);

    boolean isValidRefreshToken(String tokenId);
}