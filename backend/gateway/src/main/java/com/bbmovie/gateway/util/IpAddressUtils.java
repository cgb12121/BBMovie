package com.bbmovie.gateway.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Optional;

@Log4j2
public class IpAddressUtils {

    private IpAddressUtils() {}

    public static String getClientIp(ServerHttpRequest request) {
        return Optional.ofNullable(request.getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("anonymous");
    }

    // might use these to check ip
    private static final String[] PUBLIC_IP_SERVICES = {
            "https://checkip.amazonaws.com",
            "https://api.ipify.org",
            "https://icanhazip.com",
    };
}
