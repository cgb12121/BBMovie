package bbmovie.commerce.payment_orchestrator_service.infrastructure.idempotency;

import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.IdempotencyResult;
import bbmovie.commerce.payment_orchestrator_service.application.usecase.support.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Test
    void should_execute_join_point_for_first_execution() throws Throwable {
        IdempotencyAspect aspect = new IdempotencyAspect(idempotencyService);
        Idempotent annotation = Fixture.class.getDeclaredMethod("checkout", String.class, Object.class)
                .getAnnotation(Idempotent.class);

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"key-1", new Object()});
        when(proceedingJoinPoint.proceed()).thenReturn("live-response");
        when(idempotencyService.execute(eq(IdempotencyOperation.CHECKOUT), eq("key-1"), any(), eq(String.class), any()))
                .thenAnswer(invocation -> {
                    IdempotencyService.ThrowingSupplier<Object> supplier = invocation.getArgument(4);
                    Object value = supplier.get();
                    return new IdempotencyResult<>(false, value);
                });

        Object result = aspect.applyIdempotency(proceedingJoinPoint, annotation);

        assertEquals("live-response", result);
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    void should_return_cached_response_without_proceeding() throws Throwable {
        IdempotencyAspect aspect = new IdempotencyAspect(idempotencyService);
        Idempotent annotation = Fixture.class.getDeclaredMethod("checkout", String.class, Object.class)
                .getAnnotation(Idempotent.class);

        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"key-1", new Object()});
        when(idempotencyService.execute(eq(IdempotencyOperation.CHECKOUT), eq("key-1"), any(), eq(String.class), any()))
                .thenReturn(new IdempotencyResult<>(true, "cached-response"));

        Object result = aspect.applyIdempotency(proceedingJoinPoint, annotation);

        assertEquals("cached-response", result);
        verify(proceedingJoinPoint, never()).proceed();
    }

    static class Fixture {
        @Idempotent(operation = IdempotencyOperation.CHECKOUT, responseType = String.class)
        public String checkout(String key, Object request) {
            return "ignored";
        }
    }
}

