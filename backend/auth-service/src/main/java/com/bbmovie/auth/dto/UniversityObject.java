package com.bbmovie.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UniversityObject {
    private String name;

    private List<String> domains;

    private List<String> web_pages;

    @JsonProperty("web_page")
    private void setWebPage(List<String> web_page) {
        this.web_pages = web_page;
    }

    private String country;

    private String alpha_two_code;


    @JsonProperty("state-province")
    private String state_province;
}