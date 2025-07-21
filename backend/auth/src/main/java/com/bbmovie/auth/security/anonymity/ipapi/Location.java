package com.bbmovie.auth.security.anonymity.ipapi;

import lombok.Data;

import java.util.Date;

@Data
public class Location {
    private boolean is_eu_member;
    private String calling_code;
    private String currency_code;
    private String continent;
    private String country;
    private String country_code;
    private String state;
    private String city;
    private double latitude;
    private double longitude;
    private String zip;
    private String timezone;
    private Date local_time;
    private int local_time_unix;
    private boolean is_dst;
}