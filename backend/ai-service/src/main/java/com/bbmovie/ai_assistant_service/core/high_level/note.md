This high_level package is not being used or developed further. 
Please use the low_level package for all new developments and implementations.

We will wait until langchain4j is more mature or at least more flexible, well integrated into the Spring ecosystem 
before considering any further high_level abstractions. 

Please go to the low_level package for absolute control over the AI workflow solutions.

The default setting disables bean creation for high_level components. To enable them, set the property:
```yaml
high_level.enabled=true # Enable high_level package components, and disable low_level beans creation.
```
```java
@Configuration
public class AiModeConfiguration {

    @Configuration
    @ConditionalOnProperty(name = "ai.mode", havingValue = "high-level", matchIfMissing = true)
    @ComponentScan(basePackages = "com.bbmovie.ai_assistant_service.core.high_level")
    static class HighLevelMode { }

    @Configuration
    @ConditionalOnProperty(name = "ai.mode", havingValue = "low-level")
    @ComponentScan(basePackages = "com.bbmovie.ai_assistant_service.core.low_level")
    static class LowLevelMode { }
}
```

Thank you for your understanding!