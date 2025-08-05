package com.bbmovie.auth.security.anonymity.vpnapi;

import lombok.Data;

/**
 * The {@code Security} class represents the security attributes associated with an IP address,
 * typically used to determine whether the IP address exhibits anonymity characteristics such
 * as being associated with a VPN, proxy, Tor network, or relay.
 * <p>
 * Instances of this class are often part of responses from IP anonymity-related APIs, and the
 * boolean properties in this class indicate the presence or absence of specific anonymity-related
 * mechanisms for the IP.
 * <p>
 * Fields:
 * <p> - vpn: Indicates whether the IP address is associated with a VPN.
 * <p> - proxy: Indicates whether the IP address is associated with a proxy server.
 * <p> - tor: Indicates whether the IP address is part of the Tor network.
 * <p> - relay: Indicates whether the IP address is part of a relay mechanism.
 */
@Data
public class Security {
    private boolean vpn;
    private boolean proxy;
    private boolean tor;
    private boolean relay;
}