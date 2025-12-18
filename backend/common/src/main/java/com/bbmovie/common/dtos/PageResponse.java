package com.bbmovie.common.dtos;

import java.util.List;

public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
    
    public boolean isFirst() {
        return page == 0;
    }
    
    public boolean isLast() {
        return !hasNext;
    }


    public PageResponse() {}

    public PageResponse(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public Builder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder<T> hasNext(boolean hasNext) {
            this.hasNext = hasNext;
            return this;
        }

        public Builder<T> hasPrevious(boolean hasPrevious) {
            this.hasPrevious = hasPrevious;
            return this;
        }

        public PageResponse<T> build() {
            return new PageResponse<>(
                    content,
                    page,
                    size,
                    totalElements,
                    totalPages,
                    hasNext,
                    hasPrevious
            );
        }
    }
}