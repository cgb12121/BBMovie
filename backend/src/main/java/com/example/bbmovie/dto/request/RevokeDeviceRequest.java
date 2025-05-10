package com.example.bbmovie.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class RevokeDeviceRequest {
    private List<DeviceRevokeEntry> devices;
}
