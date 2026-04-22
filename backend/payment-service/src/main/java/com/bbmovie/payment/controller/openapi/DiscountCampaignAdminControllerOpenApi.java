package com.bbmovie.payment.controller.openapi;

import com.bbmovie.payment.dto.ApiResponse;
import com.bbmovie.payment.dto.request.DiscountCampaignCreateRequest;
import com.bbmovie.payment.dto.request.DiscountCampaignUpdateRequest;
import com.bbmovie.payment.dto.response.DiscountCampaignResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Campaign Admin", description = "Admin APIs for discount campaign management")
public interface DiscountCampaignAdminControllerOpenApi {
    @Operation(summary = "Create campaign", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<DiscountCampaignResponse> create(@Valid @RequestBody DiscountCampaignCreateRequest request);

    @Operation(summary = "Update campaign", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<DiscountCampaignResponse> update(@PathVariable("id") UUID id, @Valid @RequestBody DiscountCampaignUpdateRequest request);

    @Operation(summary = "Get campaign by ID", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<DiscountCampaignResponse> get(@PathVariable("id") UUID id);

    @Operation(summary = "Delete campaign", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<Void> delete(@PathVariable("id") UUID id);

    @Operation(summary = "List campaigns", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<List<DiscountCampaignResponse>> listAll();
}

