package bbmovie.commerce.payment_orchestrator_service.application.usecase.support;

import bbmovie.commerce.payment_orchestrator_service.application.exception.IdempotencyConflictException;
import bbmovie.commerce.payment_orchestrator_service.application.port.outbound.idempotency.IdempotencyPort;
import bbmovie.commerce.payment_orchestrator_service.application.port.result.IdempotencyResult;
import bbmovie.commerce.payment_orchestrator_service.application.config.IdempotencyOperation;
import bbmovie.commerce.commerce_common.crypto.Sha256Hasher;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.json.JsonSerdeUtils;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.entity.IdempotencyRecordEntity;
import bbmovie.commerce.payment_orchestrator_service.infrastructure.persistence.jpa.IdempotencyRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository repo;
    private final ObjectMapper objectMapper;
    private final IdempotencyPort cache;

    public <T> IdempotencyResult<T> execute(
        IdempotencyOperation op, String key, Object request,
        Class<T> responseType, ThrowingSupplier<T> action
    ) {
        Objects.requireNonNull(op, "op");
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }

        String requestJson = writeJson(request);
        String requestHash = Sha256Hasher.sha256Hex(requestJson);

        var cached = cache.get(op, key, responseType);
        if (cached.isPresent()) {
            return new IdempotencyResult<>(true, cached.get());
        }

        var existing = repo.findByOperationAndIdempotencyKey(op, key);
        if (existing.isPresent()) {
            return replayFromRecord(existing.get(), requestHash, responseType, op, key);
        }

        T result = action.get();
        return saveResult(op, key, requestHash, result, responseType);
    }

    private <T> IdempotencyResult<T> replayFromRecord(
            IdempotencyRecordEntity record,
            String requestHash,
            Class<T> responseType,
            IdempotencyOperation op,
            String key
    ) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException("Idempotency key reuse with different payload");
        }
        if (record.getResponseJson() == null) {
            throw new IdempotencyConflictException("Request is being processed. Please retry later.");
        }
        T value = readJson(record.getResponseJson(), responseType);
        putCacheBestEffort(op, key, value);
        return new IdempotencyResult<>(true, value);
    }

    private <T> IdempotencyResult<T> saveResult(
            IdempotencyOperation op,
            String key,
            String requestHash,
            T result,
            Class<T> responseType
    ) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setOperation(op);
        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setResponseJson(writeJson(result));
        record.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        try {
            repo.saveAndFlush(record);
            putCacheBestEffort(op, key, result);
            return new IdempotencyResult<>(false, result);
        } catch (DataIntegrityViolationException e) {
            return repo.findByOperationAndIdempotencyKey(op, key)
                    .map(existing -> replayFromRecord(existing, requestHash, responseType, op, key))
                    .orElseThrow(() -> e);
        }
    }

    private <T> void putCacheBestEffort(IdempotencyOperation op, String key, T value) {
        try {
            cache.put(op, key, value);
        } catch (RuntimeException e) {
            log.warn("Idempotency cache write failed for op={} key={}", op, key, e);
        }
    }

    private String writeJson(Object o) {
        return JsonSerdeUtils.write(objectMapper, o, "Failed to serialize JSON");
    }

    private <T> T readJson(String json, Class<T> type) {
        return JsonSerdeUtils.read(objectMapper, json, type, "Failed to deserialize JSON");
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get();
    }
}

