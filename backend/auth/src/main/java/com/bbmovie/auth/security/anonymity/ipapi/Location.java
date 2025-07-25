package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class Location {
    @JsonProperty("is_eu_member")
    private boolean isEuMember;
    @JsonProperty("calling_code")
    private String callingCode;
    @JsonProperty("currency_code")
    private String currencyCode;
    private String continent;
    private String country;
    @JsonProperty("country_code")
    private String countryCode;
    private String state;
    private String city;
    private double latitude;
    private double longitude;
    private String zip;
    private String timezone;
    @JsonProperty("local_time")
    private Date localTime;
    @JsonProperty("local_time_unix")
    private int localTimeUnix;
    @JsonProperty("is_dst")
    private boolean isDst;
}