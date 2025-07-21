package com.bbmovie.auth.security.anonymity.location;

import java.util.Optional;

public interface LocationService {
    Optional<String> getCountryCodeByIp(String ip);
}
