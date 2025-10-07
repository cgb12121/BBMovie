package com.example.bbmoviesearch.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@Builder
public class SearchCriteria {
    //abac, age might be removed
    private Integer age;
    private String region;

    //normal query/filter
    private String query;
    private String[] genres; //user might select multiple genes
    private String[] categories; //this is genes?
    private Integer yearFrom; //range or just release year?
    private Integer yearTo;
    private String sortBy; // enum? newest, viewcount, rating?
    private String sortOrder; // acs, des?
    private String movieType; // enum? single movie/series

    //should be fixed or might be changed for different devices
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer size = 20;
}