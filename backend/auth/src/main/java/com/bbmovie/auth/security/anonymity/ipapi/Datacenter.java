package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a data center and its related geographic and network attributes.
 * <p>
 * This class is primarily used to encapsulate information about a specific data center,
 * including its name, associated network, and location details such as country, region, and city.
 * <p>
 * Attributes:
 * <p> - `dataCenter`: The name or identifier of the data center.
 * <p> - `network`: The network associated with the data center.
 * <p> - `country`: The country in which the data center is located.
 * <p> - `region`: The region or state within the country where the data center resides.
 * <p> - `city`: The city where the data center is situated.
 */
@Data
@SuppressWarnings("squid:S1700")
public class Datacenter {
    @JsonProperty("data_center")
    private String dataCenter;
    private String network;
    private String country;
    private String region;
    private String city;
}