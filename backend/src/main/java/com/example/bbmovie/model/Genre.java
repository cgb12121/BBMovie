package com.example.bbmovie.model;

import com.example.bbmovie.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "genres")
@Getter
@Setter
@ToString
public class Genre extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "tmdb_id")
    private String tmdbId;
} 