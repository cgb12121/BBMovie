package com.example.bbmovie.security.anonymity.ipapi;

import lombok.Data;

@Data
public class Datacenter {
    private String datacenter;
    private String network;
    private String country;
    private String region;
    private String city;
}