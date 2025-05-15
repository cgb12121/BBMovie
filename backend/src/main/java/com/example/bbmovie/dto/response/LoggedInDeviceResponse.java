package com.example.bbmovie.dto.response;

public record LoggedInDeviceResponse(
    String deviceName,
    String ipAddress,
    boolean current
) {}