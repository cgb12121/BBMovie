package com.bbmovie.watchhistory.dto;

import java.util.List;

public record ResumeListPageResponse(List<ResumeResponse> items, String nextCursor) {
}
