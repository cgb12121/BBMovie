2025-10-31T17:08:01.178+07:00  INFO 1736 --- [ai-assistant-service] [           main] o.s.v.b.OptionalValidatorFactoryBean     : Failed to set up a Bean Validation provider: jakarta.validation.NoProviderFoundException: Unable to create a Configuration, because no Jakarta Bean Validation provider could be found. Add a provider like Hibernate Validator (RI) to your classpath.
2025-10-31T17:08:01.220+07:00  WARN 1736 --- [ai-assistant-service] [           main] onfigReactiveWebServerApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'webHandler' defined in class path resource [org/springframework/boot/autoconfigure/web/reactive/WebFluxAutoConfiguration$EnableWebFluxConfiguration.class]: Error creating bean with name 'requestMappingHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/reactive/WebFluxAutoConfiguration$EnableWebFluxConfiguration.class]: Ambiguous mapping. Cannot map '_SessionController' method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#archiveSession(UUID)
to {DELETE /session}: There is already '_SessionController' bean method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#deleteSession(UUID) mapped.

org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'webHandler' defined in class path resource [org/springframework/boot/autoconfigure/web/reactive/WebFluxAutoConfiguration$EnableWebFluxConfiguration.class]: Error creating bean with name 'requestMappingHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/reactive/WebFluxAutoConfiguration$EnableWebFluxConfiguration.class]: Ambiguous mapping. Cannot map '_SessionController' method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#archiveSession(UUID)
to {DELETE /session}: There is already '_SessionController' bean method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#deleteSession(UUID) mapped.

Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'requestMappingHandlerMapping' defined in class path resource [org/springframework/boot/autoconfigure/web/reactive/WebFluxAutoConfiguration$EnableWebFluxConfiguration.class]: Ambiguous mapping. Cannot map '_SessionController' method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#archiveSession(UUID)
to {DELETE /session}: There is already '_SessionController' bean method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#deleteSession(UUID) mapped.

Caused by: java.lang.IllegalStateException: Ambiguous mapping. Cannot map '_SessionController' method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#archiveSession(UUID)
to {DELETE /session}: There is already '_SessionController' bean method
com.bbmovie.ai_assistant_service.core.low_level._controller._SessionController#deleteSession(UUID) mapped.