package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("squid:S1700")
public class Asn {
    private int asn;
    @JsonProperty("abuser_score")
    private String abuserScore;
    private String route;
    private String descr;
    private String country;
    private boolean active;
    private String org;
    private String domain;
    private String abuse;
    private String type;
    private String created;
    private String updated;
    private String rir;
    private String whois;
}