package bbmovie.commerce.payment_gateway.adapter.inbound.rest;

import bbmovie.commerce.payment_gateway.dto.PurchaseStartRequest;
import bbmovie.commerce.payment_gateway.dto.PurchaseStartResponse;
import bbmovie.commerce.payment_gateway.dto.PurchaseStatusResponse;
import bbmovie.commerce.payment_gateway.service.PurchaseGatewayService;
import com.bbmovie.common.dtos.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gateway/payments")
public class PaymentGatewayController {
    private final PurchaseGatewayService purchaseGatewayService;

    @PostMapping("/purchase")
    public ApiResponse<PurchaseStartResponse> startPurchase(
            @RequestHeader(name = "Idempotency-Key", required = true) String idempotencyKey,
            @Valid @RequestBody PurchaseStartRequest request
    ) {
        return ApiResponse.success(
                purchaseGatewayService.startPurchase(idempotencyKey, request),
                "Purchase initiated"
        );
    }

    @GetMapping("/purchase/{gatewayRequestId}/status")
    public ApiResponse<PurchaseStatusResponse> getStatus(@PathVariable String gatewayRequestId) {
        return ApiResponse.success(purchaseGatewayService.getStatus(gatewayRequestId), "Purchase status");
    }
}
