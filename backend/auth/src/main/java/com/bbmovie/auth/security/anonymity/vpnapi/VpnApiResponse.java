package com.bbmovie.auth.security.anonymity.vpnapi;

import lombok.Data;

/**
 * The VpnApiResponse class represents the response data obtained from an external VPN API.
 * It contains information about the IP address, security characteristics, location details,
 * and network-related metadata of the queried IP.
 * <p>
 * This class is primarily used to hold data parsed from the API response, which can then
 * be further processed by other components or services.
 * <p>
 * Fields:
 * <p> - ip: The IP address being queried.
 * <p> - security: An object representing the security characteristics of the IP, such as whether
 *   it is associated with VPNs, proxies, Tor, or relays.
 * <p> - location: An object containing detailed information about the geographical location of
 *   the IP, including city, region, country, and more.
 * <p> - network: An object containing network-related details like the network name and
 *   autonomous system information associated with the IP.
 */
@Data
public class VpnApiResponse {
    private String ip;
    private Security security;
    private Location location;
    private Network network;
}