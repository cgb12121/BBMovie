package com.bbmovie.auth.dto.response;

public record LoggedInDeviceResponse(
    String deviceName,
    String ipAddress,
    boolean current
) {}