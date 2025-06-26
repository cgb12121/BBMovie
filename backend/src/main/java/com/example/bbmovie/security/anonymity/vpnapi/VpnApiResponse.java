package com.example.bbmovie.security.anonymity.vpnapi;

import lombok.Data;

@Data
public class VpnApiResponse {
    private String ip;
    private Security security;
    private Location location;
    private Network network;
}