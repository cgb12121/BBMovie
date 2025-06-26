package com.example.bbmovie.security.anonymity.ipapi;

import lombok.Data;

@Data
public class Company {
    private String name;
    private String abuser_score;
    private String domain;
    private String type;
    private String network;
    private String whois;
}