package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Represents geographic and administrative information associated with a location.
 * The class uses JSON properties for mapping external data keys to its internal members.
 * It captures key details about a location, including a continent, country, state, city, and
 * various attributes indicating the geographic and time-related properties of the location.
 * <p>
 * Attributes:
 * <p> - `isEuMember`: Indicates whether the location is part of the European Union.
 * <p> - `callingCode`: The international calling code for the country.
 * <p> - `currencyCode`: The currency code used in the location.
 * <p> - `continent`: The continent where the location resides.
 * <p> - `country`: The country name.
 * <p> - `countryCode`: The ISO 3166-1 alpha-2 country code.
 * <p> - `state`: The state or region within the country.
 * <p> - `city`: The city or town in the location.
 * <p> - `latitude`: The latitude coordinate of the location.
 * <p> - `longitude`: The longitude coordinate of the location.
 * <p> - `zip`: The postal code or ZIP code associated with the location.
 * <p> - `timezone`: The timezone identifier of the location.
 * <p> - `localTime`: The local datetime corresponding to the timezone of the location.
 * <p> - `localTimeUnix`: The Unix timestamp representation of the local time.
 * <p> - `isDst`: Indicates whether daylight saving time is observed.
 */
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