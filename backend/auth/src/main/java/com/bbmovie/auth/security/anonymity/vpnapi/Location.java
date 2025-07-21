package com.bbmovie.auth.security.anonymity.vpnapi;

import lombok.Data;

@Data
public class Location {
    private String city;
    private String region;
    private String country;
    private String continent;
    private String region_code;
    private String country_code;
    private String continent_code;
    private String latitude;
    private String longitude;
    private String time_zone;
    private String locale_code;
    private String metro_code;
    private boolean is_in_european_union;
}