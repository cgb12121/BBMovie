package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Company {
    private String name;
    @JsonProperty("abuser_score")
    private String abuserScore;
    private String domain;
    private String type;
    private String network;
    private String whois;
}