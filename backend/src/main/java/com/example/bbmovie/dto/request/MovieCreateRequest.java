package com.example.bbmovie.dto.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieCreateRequest {
    private String title;
    private String description;
    private Double rating;
    private List<String> categories;
    private MultipartFile poster;
}
