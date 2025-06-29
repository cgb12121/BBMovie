package com.example.bbmovieragrecommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RAGQuery {
    private String query;
    private int topK = 5;
}