package bbmovie.commerce.payment_orchestrator_service.infrastructure.idempotency;

import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    IdempotencyOperation operation();

    int keyArgIndex() default 0;

    int requestArgIndex() default 1;

    Class<?> responseType();
}

