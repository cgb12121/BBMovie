package bbmovie.transcode.vis;

import bbmovie.transcode.lgs.config.LgsAnalysisAutoConfiguration;
import bbmovie.transcode.vis.config.VisProcessingConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({LgsAnalysisAutoConfiguration.class, VisProcessingConfiguration.class})
public class VisApplication {

	public static void main(String[] args) {
		SpringApplication.run(VisApplication.class, args);
	}

}
