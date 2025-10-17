package com.bbmovie.auth.security.anonymity.location;

import java.util.Optional;

/**
 * LocationService provides methods to retrieve location-related information
 * based on an IP address.
 * <p>
 * This interface defines a contract for getting the country code associated
 * with a specific IP address. Implementations of this interface may use different
 * external services or APIs to fetch the corresponding data.
 */
@Deprecated(forRemoval = true, since = "1.0.0")
public interface LocationService {
    Optional<String> getCountryCodeByIp(String ip);
}
