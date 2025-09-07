package com.bbmovie.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.paypal")
public class PayPalProperties {
    private String clientId;
    private String clientSecret;
    private String mode;
    private String returnUrl;
}