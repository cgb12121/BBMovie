package com.bbmovie.personalizationrecommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PersonalizationRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalizationRecommendationApplication.class, args);
    }

}
