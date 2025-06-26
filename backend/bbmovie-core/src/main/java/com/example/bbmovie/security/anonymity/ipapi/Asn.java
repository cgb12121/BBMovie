package com.example.bbmovie.security.anonymity.ipapi;

import lombok.Data;

@Data
public class Asn {
    private int asn;
    private String abuser_score;
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