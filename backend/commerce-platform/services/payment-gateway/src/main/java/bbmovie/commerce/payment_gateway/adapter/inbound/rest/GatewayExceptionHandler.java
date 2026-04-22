package bbmovie.commerce.payment_gateway.adapter.inbound.rest;

import com.bbmovie.common.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.Conflict.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(HttpClientErrorException.Conflict ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Idempotency conflict: request payload differs for same Idempotency-Key"));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ApiResponse<Void>> handleClientError(HttpClientErrorException ex) {
        String message = ex.getResponseBodyAsString();
        if (message == null || message.isBlank()) {
            message = ex.getStatusCode().toString();
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(message));
    }
}
