package com.example.bbmovie.dto.request;

import lombok.Data;

@Data
public class DeviceRevokeEntry {
    private String deviceName;
    private String ip;
}