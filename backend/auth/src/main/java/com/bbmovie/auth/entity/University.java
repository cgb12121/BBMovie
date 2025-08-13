package com.bbmovie.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "universities",
        indexes = {
                @Index(name = "idx_name", columnList = "name", unique = false),
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class University {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "domains", columnDefinition = "TEXT")
    private String domains;

    @Column(name = "web_pages", columnDefinition = "TEXT")
    private String webPages;

    @Column(length = 100)
    private String country;

    @Column(length = 2)
    private String alphaTwoCode;

    @Column(length = 100)
    private String stateProvince;
}