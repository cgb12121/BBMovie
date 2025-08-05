package com.bbmovie.auth.security.anonymity;

/**
 * The {@code IpAnonymityProvider} interface provides the structure for implementing
 * IP anonymity detection services. Implementations of this interface are expected
 * to provide functionality for determining whether an IP address exhibits anonymity
 * characteristics, such as being associated with a VPN, proxy, Tor node, or other
 * anonymity-related mechanisms.
 * <p>
 * The interface also requires implementations to specify a name for the provider,
 * which could represent the underlying service or strategy used for determining
 * anonymity.
 * <p>
 * Methods:
 * <p>- {@code isAnonymity(String ip)}: Checks if the given IP address is identified as anonymous.
 * <p>- {@code getName()}: Retrieves the name of the IP anonymity provider implementation.
 */
public interface IpAnonymityProvider {
    boolean isAnonymity(String ip);
    String getName();
}
