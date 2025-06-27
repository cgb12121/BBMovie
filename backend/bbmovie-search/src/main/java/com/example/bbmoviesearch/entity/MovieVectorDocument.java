package com.example.bbmoviesearch.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "#{@environment.getProperty('spring.ai.vectorstore.elasticsearch.index-name')}")
@Data
@Builder
public class MovieVectorDocument {
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private float[] contentVector;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Keyword)
    private List<String> categories;

    @Field(type = FieldType.Keyword)
    private String posterUrl;

    @Field(type = FieldType.Date, storeNullValue = true)
    private LocalDateTime releaseDate;
}