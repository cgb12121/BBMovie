package com.bbmovie.auth.security.anonymity.vpnapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Location {
    private String city;
    private String region;
    private String country;
    private String continent;
    @JsonProperty("region_code")
    private String regionCode;
    @JsonProperty("country_code")
    private String countryCode;
    @JsonProperty("continent_code")
    private String continentCode;
    private String latitude;
    private String longitude;
    @JsonProperty("time_zone")
    private String timeZone;
    @JsonProperty("locale_code")
    private String localeCode;
    @JsonProperty("metro_code")
    private String metroCode;
    private boolean isInEuropeanUnion;
}