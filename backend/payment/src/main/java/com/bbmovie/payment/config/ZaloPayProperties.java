package com.bbmovie.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.zalopay")
public class ZaloPayProperties {
    private int version;
    private String appId;
    private String key1;
    private String key2;
    private boolean sandbox;
    private String redirectUrl;
    private String callbackUrl;
}
