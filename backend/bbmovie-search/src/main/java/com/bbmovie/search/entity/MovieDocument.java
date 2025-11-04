package com.bbmovie.search.entity;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "#{@environment.getProperty('spring.ai.vectorstore.elasticsearch.index-name')}")
@Data
@Getter
@Setter
@Builder
public class MovieDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> genres;

    @Field(type = FieldType.Keyword)
    private List<String> actors;

    @Field(type = FieldType.Keyword)
    private List<String> directors;

    @Field(type = FieldType.Integer)
    private int releaseYear;

    @Field(type = FieldType.Keyword)
    private String poster;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Date, storeNullValue = true)
    private LocalDateTime releaseDate;

    @Field(type = FieldType.Dense_Vector, dims = 384)
    private float[] embedding;
}
