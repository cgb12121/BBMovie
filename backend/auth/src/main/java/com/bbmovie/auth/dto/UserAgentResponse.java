package com.bbmovie.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAgentResponse {
    private String deviceName;

    private String deviceOs;

    private String deviceIpAddress;

    private String browser;

    private String browserVersion;
}
