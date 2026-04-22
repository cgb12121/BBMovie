package com.bbmovie.gateway.config.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "bucket4j")
public class Bucket4jConfigProperties {
    
    private boolean enabled = true;
    private String defaultHttpContentType = "application/json";
    private int defaultHttpStatusCode = 429;
    private String defaultHttpResponseBody = "{ \"message\": \"Too many requests\" }";
    private List<FilterConfig> filters = new ArrayList<>();
    
    @Data
    public static class FilterConfig {
        private String url;
        private String filterMethod = "gateway";
        private List<RateLimitConfig> rateLimits = new ArrayList<>();
    }
    
    @Data
    public static class RateLimitConfig {
        private String plan; // ANONYMOUS, FREE, PREMIUM, etc.
        private List<BandwidthConfig> bandwidths = new ArrayList<>();
    }
    
    @Data
    public static class BandwidthConfig {
        private long capacity;
        private long time;
        private String unit;
    }
}