//package com.bbmovie.gateway.security.ratelimit.keyresolver;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import reactor.core.publisher.Mono;
//
//import static com.example.common.entity.JoseConstraint.JosePayload.JTI;
//import static com.example.common.entity.JoseConstraint.JosePayload.SID;
//
//@Component("joseKeyResolver")
//public class JoseKeyResolver implements KeyResolver {
//
//    private final ObjectMapper objectMapper;
//
//    @Autowired
//    public JoseKeyResolver(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    public Mono<String> resolve(ServerWebExchange exchange) {
//        String jwt = extractJwtFromRequest(exchange);
//
//        return Mono.fromCallable(() -> {
//            JsonNode  jwtPayload = objectMapper.readTree(jwt);
//            String jti = jwtPayload.findValue(JTI).asText();
//            String sid = jwtPayload.findValue(SID).asText();
//            return jti + sid;
//        });
//    }
//
//    private String extractJwtFromRequest(ServerWebExchange exchange) {
//        String bearerToken = exchange.getRequest().getHeaders().getFirst("Authorization");
//        return (bearerToken != null && bearerToken.startsWith("Bearer "))
//                ? bearerToken.substring(7)
//                : null;
//    }
//}
