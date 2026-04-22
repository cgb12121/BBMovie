package bbmovie.commerce.billing_ledger_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class BillingLedgerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingLedgerServiceApplication.class, args);
	}

}
