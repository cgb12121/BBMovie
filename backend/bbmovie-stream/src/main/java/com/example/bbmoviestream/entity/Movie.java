package com.example.bbmoviestream.entity;

import com.example.common.enums.Storage;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "movies")
@Getter
@Setter
@ToString
@Builder
public class Movie {
    @Id
    private String id;
    @Column
    private String title;
    @Column
    private String filename;
    @Column
    private String filePath;
    @Column
    private Storage storage;
}