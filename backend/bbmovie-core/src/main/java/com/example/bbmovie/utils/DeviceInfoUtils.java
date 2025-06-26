package com.example.bbmovie.utils;

import com.example.bbmovie.dto.response.UserAgentResponse;
import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgent;
import org.springframework.stereotype.Component;

@Component
public class DeviceInfoUtils {

    private final UserAgentAnalyzerUtils userAgentAnalyzer;

    public DeviceInfoUtils(UserAgentAnalyzerUtils userAgentAnalyzer) {
        this.userAgentAnalyzer = userAgentAnalyzer;
    }

    public UserAgentResponse extractUserAgentInfo(HttpServletRequest request) {
        String userAgentString = request.getHeader("User-Agent");
        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        String deviceName = agent.getValue("DeviceName");
        String deviceIpAddress = IpAddressUtils.getClientIp(request);
        String deviceOs = agent.getValue("OperatingSystemName");
        String browser = agent.getValue("AgentName");
        String browserVersion = agent.getValue("AgentVersion");

        return new UserAgentResponse(deviceName, deviceIpAddress, deviceOs, browser, browserVersion);
    }
}
