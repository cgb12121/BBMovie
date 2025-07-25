package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("squid:S1700")
public class Datacenter {
    @JsonProperty("datacenter")
    private String dataCenter;
    private String network;
    private String country;
    private String region;
    private String city;
}