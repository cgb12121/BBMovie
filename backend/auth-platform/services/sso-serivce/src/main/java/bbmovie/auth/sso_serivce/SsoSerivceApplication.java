package bbmovie.auth.sso_serivce;

import bbmovie.auth.sso_serivce.config.CutoverProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CutoverProperties.class)
public class SsoSerivceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsoSerivceApplication.class, args);
    }

}
