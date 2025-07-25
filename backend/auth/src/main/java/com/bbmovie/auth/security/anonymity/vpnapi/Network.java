package com.bbmovie.auth.security.anonymity.vpnapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@SuppressWarnings("squid:S1700")
public class Network {
    private String network;
    @JsonProperty("autonomous_system_number")
    private String autonomousSystemNumber;
    @JsonProperty("autonomous_system_organization")
    private String autonomousSystemOrganization;
}