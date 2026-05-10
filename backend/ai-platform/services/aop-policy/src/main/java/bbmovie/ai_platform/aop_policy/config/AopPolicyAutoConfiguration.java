package bbmovie.ai_platform.aop_policy.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "bbmovie.ai_platform.aop_policy")
public class AopPolicyAutoConfiguration {
}
