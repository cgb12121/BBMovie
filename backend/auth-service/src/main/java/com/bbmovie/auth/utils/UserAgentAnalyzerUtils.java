package com.bbmovie.auth.utils;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;

@Component
public class UserAgentAnalyzerUtils {

    private final UserAgentAnalyzer analyzer;

    public UserAgentAnalyzerUtils() {
        this.analyzer = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(10000)
                .withField("DeviceName")
                .withField("DeviceBrand")
                .withField("OperatingSystemName")
                .withField("OperatingSystemVersion")
                .withField("AgentName")
                .withField("AgentVersion")
                .build();
    }

    public UserAgent parse(String userAgent) {
        return analyzer.parse(userAgent);
    }
}

