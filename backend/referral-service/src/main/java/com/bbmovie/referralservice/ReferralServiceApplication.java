package com.bbmovie.referralservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.bbmovie.referralservice.repository")
@EntityScan(basePackages = "com.bbmovie.referralservice.entity")
public class ReferralServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferralServiceApplication.class, args);
    }

}
