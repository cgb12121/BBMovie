package com.bbmovie.common.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}