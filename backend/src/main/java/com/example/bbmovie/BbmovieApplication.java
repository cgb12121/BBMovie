package com.example.bbmovie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class BbmovieApplication {

	public static void main(String[] args) {
		SpringApplication.run(BbmovieApplication.class, args);
	}

}
