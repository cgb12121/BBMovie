package com.bbmovie.mediauploadservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class MediaUploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaUploadServiceApplication.class, args);
    }

}
