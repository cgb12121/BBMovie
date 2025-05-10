package com.example.bbmovie.controller.advice;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.exception.*;
import com.example.bbmovie.exception.BlacklistedJwtTokenException;
import com.example.bbmovie.exception.InvalidTokenException;
import com.example.bbmovie.exception.TokenExpiredException;
import com.example.bbmovie.exception.TokenVerificationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.io.IOException;

@Log4j2
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NoRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoRefreshTokenException(NoRefreshTokenException e) {
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED, "No refresh token found");
    }

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(io.jsonwebtoken.ExpiredJwtException e) {
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED, "Invalid token");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e) {
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, "Invalid file format");
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return buildErrorResponse(e, HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to access this resource");
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(AccountLockedException e) {
        return buildErrorResponse(e, HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AccountNotEnabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotEnabledException(AccountNotEnabledException e) {
        return buildErrorResponse(e, HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e){
        return buildErrorResponse(e, HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyVerifiedException(EmailAlreadyVerifiedException e) {
        return buildErrorResponse(e, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegistrationException(RegistrationException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTokenException(InvalidTokenException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyVerifiedException(UserAlreadyVerifiedException ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailSendingException(EmailSendingException ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPasswordException(InvalidPasswordException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TemplateInputException.class)
    public ResponseEntity<ApiResponse<Void>> handleTemplateInputException(TemplateInputException ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(SpelEvaluationException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpelEvaluationException(SpelEvaluationException ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(TemplateProcessingException.class)
    public ResponseEntity<ApiResponse<Void>> handleTemplateProcessingException(TemplateProcessingException ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(BlacklistedJwtTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlacklistedJwtTokenException(BlacklistedJwtTokenException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, "Access token has been blocked for this email and device");
    }

    @ExceptionHandler(CustomEmailException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomEmailException(CustomEmailException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedUserException(UnauthorizedUserException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(TokenVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenVerificationException(TokenVerificationException ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(Exception ex, HttpStatus status, String message) {
        log.error("Handled exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
