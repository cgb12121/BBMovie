package com.bbmovie.watchhistory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WatchHistoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(WatchHistoryApplication.class, args);
    }

}
