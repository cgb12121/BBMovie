package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ElasticQueryBuilder {

    public void applyCriteria(BoolQuery.Builder bool, SearchCriteria criteria) {
        // --- must / filter conditions ---
        if (criteria.getAge() != null) {
            bool.must(m -> m
                .range(r -> r
                        .term(t -> t
                                .field("ageRating")
                                .lte(String.valueOf(criteria.getAge()))
                        )
                )
            );
        }

        if (criteria.getRegion() != null) {
            bool.filter(f -> f
                .term(t -> t
                    .field("region.keyword")
                    .value(criteria.getRegion())
                )
            );
        }

        if (criteria.getGenres() != null && criteria.getGenres().length > 0) {
            bool.filter(f -> f
                .terms(t -> t
                    .field("genres.keyword")
                    .terms(v -> v.value(List.of(FieldValue.of(criteria.getGenres()))))
                )
            );
        }

        if (criteria.getCategories() != null && criteria.getCategories().length > 0) {
            bool.filter(f -> f
                .terms(t -> t
                    .field("categories.keyword")
                    .terms(v -> v.value(List.of(FieldValue.of(criteria.getCategories()))))
                )
            );
        }

        if (criteria.getMovieType() != null) {
            bool.filter(f -> f
                .term(t -> t
                    .field("movieType.keyword")
                    .value(criteria.getMovieType())
                )
            );
        }

        if (criteria.getYearFrom() != null || criteria.getYearTo() != null) {
            bool.filter(f -> f
                .range(r -> {
                    RangeQuery.Builder range = (RangeQuery.Builder) new RangeQuery.Builder()
                            .term(t -> t.field("releaseYear"));
                    if (criteria.getYearFrom() != null) {
                        range.term(t -> t.gte(JsonData.of(criteria.getYearFrom()).toString()));
                    }
                    if (criteria.getYearTo() != null) {
                        range.term(t -> t.lte(JsonData.of(criteria.getYearTo()).toString()));
                    }
                    return range;
                })
            );
        }
    }

    public SortOptions buildSort(SearchCriteria criteria) {
        String sortField = switch (criteria.getSortBy()) {
            case "newest" -> "releaseDate";
            case "viewCount" -> "views";
            case "rating" -> "rating";
            default -> "";
        };

        SortOrder order = "asc".equalsIgnoreCase(criteria.getSortOrder())
                ? SortOrder.Asc
                : SortOrder.Desc;

        return new SortOptions.Builder()
                .field(f -> f.field(sortField).order(order))
                .build();
    }
}
