package com.bbmovie.payment.config.payment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.momo")
public class MomoProperties {
    private String partnerCode;
    private String storeName;
    private String storeId;
    private String accessKey;
    private String secretKey;
    private boolean sandbox;
    private String redirectUrl;
    private String ipnUrl;
}
