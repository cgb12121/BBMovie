package com.bbmovie.auth.dto.response;

import java.util.Collections;
import java.util.List;

public record UniversityLookupResponse(
    String name,
    String country,
    List<String> domains,
    boolean supported
) {
    public static UniversityLookupResponse from(String name, String country, String domain) {
        return new UniversityLookupResponse(
            name,
            country,
            domain == null ? List.of() : Collections.singletonList(domain),
            true
        );
    }
}