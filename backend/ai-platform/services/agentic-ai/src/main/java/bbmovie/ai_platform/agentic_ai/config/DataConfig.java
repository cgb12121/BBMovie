package bbmovie.ai_platform.agentic_ai.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import javax.sql.DataSource;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories(basePackages = "bbmovie.ai_platform.agentic_ai.repository")
@EnableRedisRepositories(basePackages = "bbmovie.ai_platform.agentic_ai.repository.redis")
public class DataConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
        return liquibase;
    }
}
