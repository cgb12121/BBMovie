//package com.example.bbmovie.model.elasticsearch;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//import org.springframework.data.elasticsearch.annotations.Document;
//import org.springframework.data.elasticsearch.annotations.Field;
//import org.springframework.data.elasticsearch.annotations.FieldType;
//
//import lombok.Builder;
//import lombok.Data;
//
//@Document(indexName = "#{@environment.getProperty('spring.ai.vectorstore.elasticsearch.index-name')}")
//@Data
//@Builder
//public class MovieVectorDocument {
//    @Field(type = FieldType.Keyword)
//    private String id;
//
//    @Field(type = FieldType.Text, analyzer = "standard")
//    private String title;
//
//    @Field(type = FieldType.Text, analyzer = "standard")
//    private String description;
//
//    @Field(type = FieldType.Dense_Vector, dims = 1536)
//    private float[] contentVector;
//
//    @Field(type = FieldType.Double)
//    private Double rating;
//
//    @Field(type = FieldType.Keyword)
//    private List<String> categories;
//
//    @Field(type = FieldType.Keyword)
//    private String posterUrl;
//
//    @Field(type = FieldType.Date)
//    private LocalDateTime releaseDate;
//}