package com.example.bbmovie.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Network{
    public String network;
    @JsonProperty("autonomous_system_number")
    public String autonomousSystemNumber;
    @JsonProperty("autonomous_system_organization")
    public String autonomousSystemOrganization;
}