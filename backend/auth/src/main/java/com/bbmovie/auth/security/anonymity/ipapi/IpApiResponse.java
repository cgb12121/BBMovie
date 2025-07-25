package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IpApiResponse {
    private String ip;
    private String rir;
    @JsonProperty("is_bogon")
    private boolean isBogon;
    @JsonProperty("is_mobile")
    private boolean isMobile;
    @JsonProperty("is_satellite")
    private boolean isSatellite;
    @JsonProperty("is_crawler")
    private boolean isCrawler;
    @JsonProperty("is_datacenter")
    private boolean isDatacenter;
    @JsonProperty("is_tor")
    private boolean isTor;
    @JsonProperty("is_proxy")
    private boolean isProxy;
    @JsonProperty("is_vpn")
    private boolean isVpn;
    @JsonProperty("is_abuser")
    private boolean isAbuser;
    private Datacenter datacenter;
    private Company company;
    private Abuse abuse;
    private Asn asn;
    private Location location;
    @JsonProperty("elapsed_ms")
    private double elapsedMs;
}