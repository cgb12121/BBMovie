package com.bbmovie.auth.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class RevokeDeviceRequest {
    private List<DeviceRevokeEntry> devices;
}
