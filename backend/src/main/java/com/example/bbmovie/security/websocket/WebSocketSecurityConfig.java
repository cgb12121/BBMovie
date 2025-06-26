package com.example.bbmovie.security.websocket;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authorization.AuthorizationEventPublisher;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import java.util.List;

@Log4j2
@Configuration
@EnableWebSocketSecurity
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final WebsocketJoseHandshakeInterceptor websocketJoseHandshakeInterceptor;
    private final WebSocketHandlerDecoratorFactory authWebSocketHandlerDecorator;
    private final ApplicationContext applicationContext;
    private final AuthorizationManager<Message<?>> authorizationManager;

    @Autowired
    public WebSocketSecurityConfig(
            WebsocketJoseHandshakeInterceptor websocketJoseHandshakeInterceptor,
            WebSocketHandlerDecoratorFactory authWebSocketHandlerDecorator,
            ApplicationContext applicationContext,
            AuthorizationManager<Message<?>> authorizationManager
    ) {
        this.websocketJoseHandshakeInterceptor = websocketJoseHandshakeInterceptor;
        this.authWebSocketHandlerDecorator = authWebSocketHandlerDecorator;
        this.applicationContext = applicationContext;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(websocketJoseHandshakeInterceptor)
                .setAllowedOriginPatterns(frontendUrl)
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(authWebSocketHandlerDecorator);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        AuthorizationChannelInterceptor auth = new AuthorizationChannelInterceptor(authorizationManager);
        AuthorizationEventPublisher publisher = new SpringAuthorizationEventPublisher(applicationContext);
        auth.setAuthorizationEventPublisher(publisher);
        registration.interceptors(
                messageLoggingInterceptor(),
                new SecurityContextChannelInterceptor(),
                new AuthorizationChannelInterceptor(authorizationManager)
        );
        registration.interceptors(new SecurityContextChannelInterceptor(), auth);
    }

    @Bean
    public ChannelInterceptor messageLoggingInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message,@NonNull  MessageChannel channel) {
                log.debug("Pre-send message: {}", message);
                return message;
            }

            @Override
            public void postSend(@NonNull Message<?> message,@NonNull  MessageChannel channel, boolean sent) {
                log.debug("Post-send message: {}", message);
            }
        };
    }
}