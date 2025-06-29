package com.example.bbmovieragrecommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContextSnippet {
    private String title;
    private String type; // "Movie", "Actor", etc.
    private String text;
    private double score;
}