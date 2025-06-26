package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "person_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@ToString
public abstract class Person extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(name = "death_date")
    private java.time.LocalDate deathDate;
} 