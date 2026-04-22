package com.bbmovie.camundaengine.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityMatch {
    private String name;
    private String domain;
    private String country;
    private double confidence;
    private boolean matched;
}
