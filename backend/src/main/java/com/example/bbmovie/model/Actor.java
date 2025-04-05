package com.example.bbmovie.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@DiscriminatorValue("ACTOR")
@Getter
@Setter
@ToString
public class Actor extends Person {

}