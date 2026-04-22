package com.bbmovie.camundaengine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "universities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class University extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "domains", length = 1000)
    private String domains;

    @Column(name = "web_pages", length = 1000)
    private String webPages;

    @Column(name = "country")
    private String country;

    @Column(name = "alpha_two_code")
    private String alphaTwoCode;

    @Column(name = "state_province")
    private String stateProvince;
}
