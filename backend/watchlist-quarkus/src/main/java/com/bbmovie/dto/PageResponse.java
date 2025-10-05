package com.bbmovie.dto;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

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
    public static <T> PageResponse<T> from(PanacheQuery<T> query) {
        Page p = query.page();
        List<T> items = query.list();
        long total = query.count();
        int totalPages = (int) Math.ceil((double) total / p.size);

        return new PageResponse<>(
                items,
                p.index,
                p.size,
                total,
                totalPages,
                p.index + 1 < totalPages,
                p.index > 0,
                p.index + 1 < totalPages
                        ? p.index + 1
                        : null,
                p.index > 0
                        ? p.index - 1
                        : null
        );
    }
}