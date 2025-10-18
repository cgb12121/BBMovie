package com.bbmovie.gateway.security.anonymity.ipapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a company and its associated information.
 * <p>
 * This class is designed to capture various attributes of a company,
 * including its name, domain, type, network, and WHOIS information.
 * A specific property, `abuserScore`, is used to quantify abusive behavior
 * or activity linked to the company, and is serialized/deserialized using the `abuser_score` key
 * in JSON representations.
 */
@Data
public class Company {
    private String name;
    @JsonProperty("abuser_score")
    private String abuserScore;
    private String domain;
    private String type;
    private String network;
    private String whois;
}