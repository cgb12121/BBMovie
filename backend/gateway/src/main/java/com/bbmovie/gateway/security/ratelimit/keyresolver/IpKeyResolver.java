//package com.bbmovie.gateway.security.ratelimit.keyresolver;
//
//import com.bbmovie.gateway.exception.IpAddressException;
//import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import java.util.Objects;
//
//@Component("ipKeyResolver")
//public class IpKeyResolver implements KeyResolver {
//
//    @Override
//    public Mono<String> resolve(ServerWebExchange exchange) {
//        return Mono.just(getClientIp(exchange));
//    }
//
//    private String getClientIp(ServerWebExchange exchange) {
//        try {
//            return Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
//                    .getAddress()
//                    .getHostAddress();
//        } catch (Exception e) {
//            throw new IpAddressException("Cannot get client ip: " + e.getMessage());
//        }
//    }
//}