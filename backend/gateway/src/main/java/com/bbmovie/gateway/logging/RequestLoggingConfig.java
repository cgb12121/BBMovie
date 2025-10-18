package com.bbmovie.gateway.logging;

import com.bbmovie.gateway.config.FilterOrder;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class RequestLoggingConfig implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        var uri = request.getURI();
        HttpHeaders headers = request.getHeaders();
        MediaType contentType = headers.getContentType();

        log.info("Request: {} {}", method, uri);
        log.info("Headers: {}", headers);
        log.info("Cookies: {}", request.getCookies());

        // Log response
        ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            @NonNull
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            String responseBody = new String(bytes, StandardCharsets.UTF_8);
                            log.info("Response Body: {}", responseBody);

                            DataBuffer buffer = getDelegate().bufferFactory().wrap(bytes);
                            return getDelegate().writeWith(Mono.just(buffer));
                        });
            }
        };

        if ((method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)
                && MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return logAndRebufferRequestBody(exchange, chain, decoratedResponse);
        }

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    private Mono<Void> logAndRebufferRequestBody(ServerWebExchange exchange, GatewayFilterChain chain, ServerHttpResponse decoratedResponse) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String bodyString = new String(bytes, StandardCharsets.UTF_8);
                    log.info("Body: {}", bodyString);

                    Flux<DataBuffer> cachedBody = Flux.defer(() ->
                            Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
                    );

                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        @NonNull
                        public Flux<DataBuffer> getBody() {
                            return cachedBody;
                        }
                    };

                    return chain.filter(exchange.mutate().request(decoratedRequest).response(decoratedResponse).build());
                });
    }

    @Override
    public int getOrder() {
        return FilterOrder.FIRST;
    }
}