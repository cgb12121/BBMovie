package com.example.bbmovie.security.anonymity.ipapi;

public class IpApiResponse {
    public String ip;
    public String rir;
    public boolean is_bogon;
    public boolean is_mobile;
    public boolean is_satellite;
    public boolean is_crawler;
    public boolean is_datacenter;
    public boolean is_tor;
    public boolean is_proxy;
    public boolean is_vpn;
    public boolean is_abuser;
    public Datacenter datacenter;
    public Company company;
    public Abuse abuse;
    public Asn asn;
    public Location location;
    public double elapsed_ms;
}