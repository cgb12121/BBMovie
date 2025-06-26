package com.example.bbmovie.security.anonymity.vpnapi;

import lombok.Data;

@Data
public class Network{
    private String network;
    private String autonomous_system_number;
    private String autonomous_system_organization;
}