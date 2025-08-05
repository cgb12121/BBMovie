package com.bbmovie.auth.security.anonymity.vpnapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * The {@code Network} class represents network-related metadata associated with an IP
 * address, typically retrieved from an external API that provides information about
 * IP addresses, networks, and related infrastructure.
 * <p>
 * This class contains details about the network name, its autonomous system number (ASN),
 * and the organization operating the autonomous system.
 * Instances of this class are commonly used to provide network context in relation
 * to IP addresses, enabling further analysis or categorization.
 */
@Data
@SuppressWarnings("squid:S1700")
public class Network {
    private String network;
    @JsonProperty("autonomous_system_number")
    private String autonomousSystemNumber;
    @JsonProperty("autonomous_system_organization")
    private String autonomousSystemOrganization;
}