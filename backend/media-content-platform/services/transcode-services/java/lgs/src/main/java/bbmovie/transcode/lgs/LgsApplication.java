package bbmovie.transcode.lgs;

import bbmovie.transcode.lgs.config.LgsAnalysisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(LgsAnalysisAutoConfiguration.class)
public class LgsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LgsApplication.class, args);
	}

}
