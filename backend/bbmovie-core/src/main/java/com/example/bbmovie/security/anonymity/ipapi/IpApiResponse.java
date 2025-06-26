package com.example.bbmovie.security.anonymity.ipapi;

import lombok.Data;

@Data
public class IpApiResponse {
    private String ip;
    private String rir;
    private boolean is_bogon;
    private boolean is_mobile;
    private boolean is_satellite;
    private boolean is_crawler;
    private boolean is_datacenter;
    private boolean is_tor;
    private boolean is_proxy;
    private boolean is_vpn;
    private boolean is_abuser;
    private Datacenter datacenter;
    private Company company;
    private Abuse abuse;
    private Asn asn;
    private Location location;
    private double elapsed_ms;
}