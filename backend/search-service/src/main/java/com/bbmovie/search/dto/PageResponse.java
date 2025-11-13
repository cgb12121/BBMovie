package com.bbmovie.search.dto;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious,
        Integer nextPage,
        Integer prevPage
) {
    public PageResponse() {
        this(List.of(), 0, 0, 0L, 1, false, false, null, null);
    }

    public static <T> PageResponse<T> toPageResponse(SearchResponse<T> response, int page, int size) {
        long totalItems = response.hits().total() != null
                ? response.hits().total().value()
                : 0;
        int totalPages = (int) Math.ceil((double) totalItems / size);

        List<T> items = response
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .toList();

        return new PageResponse<>(
                items,
                page,
                size,
                totalItems,
                totalPages,
                page + 1 < totalPages,
                page > 0,
                page + 1 < totalPages ? page + 1 : null,
                page > 0 ? page - 1 : null
        );
    }
}
