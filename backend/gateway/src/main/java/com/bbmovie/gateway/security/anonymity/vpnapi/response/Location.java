package com.bbmovie.gateway.security.anonymity.vpnapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * The {@code Location} class represents detailed geographical data about an IP address or entity,
 * typically retrieved from an external location-based API.
 * <p>
 * This class contains fields to hold information such as city, region, country, continent,
 * latitude, longitude, time zone, and various standardized codes used to represent those
 * geographical attributes.
 * <p>
 * Fields include:
 * <p> - city, region, country, and continent to represent the general location.
 * <p> - regionCode, countryCode, and continentCode to hold standardized codes for the location.
 * <p> - latitude and longitude for geographical coordinates.
 * <p> - timeZone to represent the time zone of the location.
 * <p> - localeCode and metroCode for specific additional regional information.
 * <p> - isInEuropeanUnion to indicate whether the location is part of the European Union.
 * <p>
 * This class is typically used as part of location-related data within API responses or
 * systems that process geographical metadata.
 */
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