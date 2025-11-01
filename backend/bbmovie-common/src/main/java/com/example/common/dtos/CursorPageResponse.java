package com.example.common.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// Infinite Scroll with Cursor-Based Pagination
@Data
@Builder
public class CursorPageResponse<T> {
    private List<T> content;
    private String nextCursor;  // Base64 encoded timestamp or ID
    private boolean hasMore;
    private int size;
}