package com.bbmovie.search.dto;

import co.elastic.clients.elasticsearch._types.SortOrder;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@SuppressWarnings("squid:S115")
@Data
@Getter
@Builder
public class SearchCriteria {
    @NotBlank @NotNull private String query;

    @Nullable private String[] categories;
    @Nullable private String country;
    @Nullable private Integer yearFrom;
    @Nullable private Integer yearTo;

    @Nullable private FilterOptions filterBy;
    @Nullable private MovieType type;
    @Nullable private SortOrderOptions sortOrder;

    //should be fixed or might be changed for different devices
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    public enum FilterOptions {
        newest, most_view, rating
    }

    public enum MovieType {
        movie, series;

        public String get() {
            return this.name().toLowerCase();
        }
    }

    public enum SortOrderOptions {
        asc, desc;

        public SortOrder get() {
            SortOrder[] esSortOrders = SortOrder.values();
            for (SortOrder esSortOrder : esSortOrders) {
                if (esSortOrder.jsonValue().equalsIgnoreCase(this.name())) {
                    return esSortOrder;
                }
            }
            return SortOrder.Desc; //default
        }
    }
}
