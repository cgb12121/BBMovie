package com.bbmovie.auth.controller.openapi;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.StudentApplicationObject;
import com.bbmovie.auth.dto.request.StudentVerificationRequest;
import com.bbmovie.auth.dto.response.StudentVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Student Program", description = "Student verification and administration APIs")
public interface StudentProgramControllerOpenApi {

    @Operation(
            summary = "Apply for student verification",
            description = "Submit student verification payload and student document",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student verification request submitted",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload/document", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    ResponseEntity<ApiResponse<StudentVerificationResponse>> apply(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestPart("payload") StudentVerificationRequest request,
            @RequestPart("document") MultipartFile document,
            Authentication authentication
    );

    @Operation(
            summary = "Get a student application",
            description = "Fetch one student application by application ID",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Application found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    })
    ResponseEntity<ApiResponse<StudentApplicationObject>> findApplicationById(
            @RequestHeader("Authorization") String authorization,
            @PathVariable UUID applicationId
    );

    @Operation(summary = "List all student applications", description = "Admin endpoint to list all applications")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Applications retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllApplications();

    @Operation(summary = "List pending applications", description = "Admin endpoint to list all pending student applications")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending applications retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllPending();

    @Operation(summary = "List rejected applications", description = "Admin endpoint to list all rejected student applications")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rejected applications retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllRejected();

    @Operation(summary = "List verified student accounts", description = "Admin endpoint to list accounts with student status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student accounts retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllStudents();

    @Operation(summary = "Manual student decision", description = "Admin endpoint to approve or reject student status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Decision applied",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    ResponseEntity<ApiResponse<StudentVerificationResponse>> manuallyValidate(
            @PathVariable UUID userId,
            @RequestParam("approve") boolean approve
    );

    @Operation(summary = "Finalize student verification", description = "Internal endpoint to finalize verification workflow")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verification finalized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status/message", content = @Content)
    })
    ResponseEntity<ApiResponse<Void>> finalizeVerification(
            @PathVariable UUID applicationId,
            @RequestParam("status") String status,
            @RequestParam(value = "message", required = false) String message
    );
}
