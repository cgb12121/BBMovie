package com.bbmovie.auth.dto.request;

import lombok.Data;

@Data
public class DeviceRevokeEntry {
    private String deviceName;
    private String ip;
}