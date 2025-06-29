package com.example.bbmovieragrecommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGResponse {
    private String answer;
    private List<ContextSnippet> snippets;
}