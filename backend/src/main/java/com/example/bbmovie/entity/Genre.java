package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
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
} 