package com.example.bbmovie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@DiscriminatorValue("DIRECTOR")
@Getter
@Setter
@ToString
public class Director extends Person {

}