package com.bbmovie.auth.dto.response;

import com.bbmovie.auth.entity.University;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

public record UniversityLookupResponse(
    String name,
    String country,
    List<String> domains,
    boolean supported
) {
    public static UniversityLookupResponse from(University u) {
        return new UniversityLookupResponse(
            u.getName(),
            u.getCountry(),
           Collections.singletonList(u.getDomains()),
            true
        );
    }

    public static Page<UniversityLookupResponse> fromPage(Page<University> universities) {
        return universities.map(u -> new UniversityLookupResponse(
            u.getName(),
            u.getCountry(),
            Collections.singletonList(u.getDomains()),
            true
        ));
    }
}