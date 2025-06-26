package com.example.bbmovie.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Location {
    public String city;
    public String region;
    public String country;
    public String continent;
    @JsonProperty("region_code")
    public String regionCode;
    @JsonProperty("country_code")
    public String countryCode;
    @JsonProperty("continent_code")
    public String continentCode;
    public String latitude;
    public String longitude;
    @JsonProperty("time_zone")
    public String timeZone;
    @JsonProperty("locale_code")
    public String localeCode;
    @JsonProperty("metro_code")
    public String metroCode;
    @JsonProperty("is_in_european_union")
    public boolean isInEuropeanUnion;
}