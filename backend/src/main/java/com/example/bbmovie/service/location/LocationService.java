package com.example.bbmovie.service.location;

import java.util.Optional;

public interface LocationService {
    Optional<String> getCountryCodeByIp(String ip);
}
