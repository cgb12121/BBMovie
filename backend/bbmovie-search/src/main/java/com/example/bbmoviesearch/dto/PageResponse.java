package com.example.bbmoviesearch.dto;

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
}