package bbmovie.auth.mfa_service;

import bbmovie.auth.mfa_service.config.CutoverProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CutoverProperties.class)
public class MfaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MfaServiceApplication.class, args);
    }

}
