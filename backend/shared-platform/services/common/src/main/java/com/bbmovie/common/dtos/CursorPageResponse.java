package com.bbmovie.common.dtos;

import java.util.List;

public class CursorPageResponse<T> {
    private List<T> content;
    private String nextCursor;  // Base64 encoded timestamp or ID
    private boolean hasMore;
    private int size;

    public CursorPageResponse() {}

    public CursorPageResponse(
            List<T> content,
            String nextCursor,
            boolean hasMore,
            int size
    ) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.size = size;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> content;
        private String nextCursor;
        private boolean hasMore;
        private int size;

        public Builder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public Builder<T> nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public Builder<T> hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public CursorPageResponse<T> build() {
            return new CursorPageResponse<>(content, nextCursor, hasMore, size);
        }
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }

    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}