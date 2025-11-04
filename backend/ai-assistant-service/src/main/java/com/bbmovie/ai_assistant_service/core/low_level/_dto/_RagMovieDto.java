package com.bbmovie.ai_assistant_service.core.low_level._dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"embedding"})
public class _RagMovieDto {

    @NonNull
    private String id;

    private String title;
    private String description;
    private String poster;

    private List<String> genres;
    private List<String> actors;
    private List<String> directors;

    private Integer duration; // in minutes
    private Integer releaseYear;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant releaseDate;

    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    @JsonAnySetter
    public void addExtra(String key, Object value) {
        if (!"embedding".equals(key)) {
            extra.put(key, value);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }
}