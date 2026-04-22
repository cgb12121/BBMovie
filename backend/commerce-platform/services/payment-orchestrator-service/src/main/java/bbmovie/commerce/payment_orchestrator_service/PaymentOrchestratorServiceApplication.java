package bbmovie.commerce.payment_orchestrator_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentOrchestratorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentOrchestratorServiceApplication.class, args);
	}

}
