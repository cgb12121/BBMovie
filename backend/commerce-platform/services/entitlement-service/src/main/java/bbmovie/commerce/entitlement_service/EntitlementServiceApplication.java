package bbmovie.commerce.entitlement_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@SpringBootApplication
public class EntitlementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EntitlementServiceApplication.class, args);
	}

}
