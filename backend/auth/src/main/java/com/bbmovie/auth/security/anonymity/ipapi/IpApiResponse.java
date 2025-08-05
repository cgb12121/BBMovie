package com.bbmovie.auth.security.anonymity.ipapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents the response entity for IP information retrieved from an IP API.
 * This class provides various details about an IP address, including basic network information,
 * usage characteristics, and associations with organizations or services.
 * <p>
 * Attributes:
 * <p> - `ip`: The IP address being queried.
 * <p> - `rir`: The Regional Internet Registry (RIR) responsible for the IP block of the queried address.
 * <p> - `isBogon`: Indicates if the IP address is within a bogon range and is not assigned for public internet use.
 * <p> - `isMobile`: Specifies whether the IP address originates from a mobile network.
 * <p> - `isSatellite`: Specifies whether the IP address originates from satellite internet services.
 * <p> - `isCrawler`: Indicates if the IP address is associated with a web crawler or bot.
 * <p> - `isDatacenter`: Specifies whether the IP is linked to datacenter hosting services.
 * <p> - `isTor`: Indicates if the IP address is associated with the Tor network for anonymous communication.
 * <p> - `isProxy`: Indicates that the IP address is being used as a proxy server.
 * <p> - `isVpn`: Indicates whether the IP is associated with a VPN service.
 * <p> - `isAbuser`: Specifies if the IP has been marked as an abusive IP address.
 * <p> - `datacenter`: Information about the datacenter associated with the IP address.
 * <p> - `company`: Information about the company that owns or operates the IP address or range.
 * <p> - `abuse`: Details about abuse reporting or response associated with the IP address.
 * <p> - `asn`: Information about the Autonomous System Number (ASN) of the IP address.
 * <p> - `location`: Geographic and administrative details of the IP's physical location.
 * <p> - `elapsedMs`: The time taken, in milliseconds, by the API to generate the response.
 */
@Data
public class IpApiResponse {
    private String ip;
    private String rir;
    @JsonProperty("is_bogon")
    private boolean isBogon;
    @JsonProperty("is_mobile")
    private boolean isMobile;
    @JsonProperty("is_satellite")
    private boolean isSatellite;
    @JsonProperty("is_crawler")
    private boolean isCrawler;
    @JsonProperty("is_datacenter")
    private boolean isDatacenter;
    @JsonProperty("is_tor")
    private boolean isTor;
    @JsonProperty("is_proxy")
    private boolean isProxy;
    @JsonProperty("is_vpn")
    private boolean isVpn;
    @JsonProperty("is_abuser")
    private boolean isAbuser;
    private Datacenter datacenter;
    private Company company;
    private Abuse abuse;
    private Asn asn;
    private Location location;
    @JsonProperty("elapsed_ms")
    private double elapsedMs;
}