package com.bbmovie.payment.config.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.vnpay")
public class VnpayProperties {
    private String payUrl;
    private String returnUrl;
    private String tmnCode;
    private String hashSecret;
    private String apiUrl;
}
