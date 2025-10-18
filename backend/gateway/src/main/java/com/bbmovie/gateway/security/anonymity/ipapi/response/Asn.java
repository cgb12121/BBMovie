package com.bbmovie.gateway.security.anonymity.ipapi.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents an Autonomous System Number (ASN) entity and its associated metadata.
 * <p>
 * The ASN class captures various attributes related to an autonomous system,
 * such as its number, route, description, associated organization, and country.
 * Additionally, it includes information about its activity status, abuse-related
 * contact details, type, and registration details such as creation and update timestamps.
 * <p>
 * The class provides fields for geographic and organizational data, reflecting
 * the characteristics and metadata commonly associated with network entities
 * in IP routing and registration systems.
 */
@Data
@SuppressWarnings("squid:S1700")
public class Asn {
    private int asn;
    @JsonProperty("abuser_score")
    private String abuserScore;
    private String route;
    private String descr;
    private String country;
    private boolean active;
    private String org;
    private String domain;
    private String abuse;
    private String type;
    private String created;
    private String updated;
    private String rir;
    private String whois;
}