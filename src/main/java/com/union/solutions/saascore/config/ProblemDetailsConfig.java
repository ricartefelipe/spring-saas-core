package com.union.solutions.saascore.config;

import com.union.solutions.saascore.adapters.in.rest.ProblemDetails;
import com.union.solutions.saascore.application.user.EmailAlreadyExistsException;
import com.union.solutions.saascore.application.user.UserAlreadyExistsException;
import com.union.solutions.saascore.domain.exception.AiServiceException;
import com.union.solutions.saascore.domain.exception.CryptoException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemDetailsConfig {

  @Value("${spring.profiles.active:local}")
  private String activeProfile;

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ProblemDetails> handleAuth(
      AuthenticationException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ProblemDetails.of(
                401,
                "Unauthorized",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetails> handleForbidden(
      AccessDeniedException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ProblemDetails.of(
                403,
                "Forbidden",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetails> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest req) {
    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ProblemDetails.of(
                400,
                "Validation Failed",
                detail,
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetails> handleBadRequest(
      IllegalArgumentException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ProblemDetails.of(
                400,
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetails> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ProblemDetails.of(
                409,
                "Conflict",
                "Data integrity violation",
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetails> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest req) {
    String detail =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining("; "));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ProblemDetails.of(
                400, "Bad Request", detail, req.getRequestURI(), TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetails> handleNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ProblemDetails.of(
                400,
                "Bad Request",
                "Malformed JSON request",
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler({EmailAlreadyExistsException.class, UserAlreadyExistsException.class})
  public ResponseEntity<ProblemDetails> handleConflict(
      RuntimeException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ProblemDetails.of(
                409,
                "Conflict",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ProblemDetails> handleIllegalState(
      IllegalStateException ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ProblemDetails.of(
                400,
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(CryptoException.class)
  public ResponseEntity<ProblemDetails> handleCrypto(CryptoException ex, HttpServletRequest req) {
    org.slf4j.LoggerFactory.getLogger(ProblemDetailsConfig.class)
        .error("Crypto error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ProblemDetails.of(
                500,
                "Internal Server Error",
                "Cryptographic operation failed",
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(AiServiceException.class)
  public ResponseEntity<ProblemDetails> handleAiService(
      AiServiceException ex, HttpServletRequest req) {
    org.slf4j.LoggerFactory.getLogger(ProblemDetailsConfig.class)
        .warn("AI service error: {}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(
            ProblemDetails.of(
                503,
                "Service Unavailable",
                "AI service error: " + ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
  public ResponseEntity<ProblemDetails> handleNotFound(Exception ex, HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ProblemDetails.of(
                404,
                "Not Found",
                ex.getMessage(),
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleOther(Exception ex, HttpServletRequest req) {
    org.slf4j.LoggerFactory.getLogger(ProblemDetailsConfig.class)
        .warn("Unhandled exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
    String detail = "An error occurred";
    if (!"prod".equals(activeProfile)) {
      detail = "An error occurred (" + ex.getClass().getSimpleName() + ": " + (ex.getMessage() != null ? ex.getMessage() : "null") + ")";
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ProblemDetails.of(
                500,
                "Internal Server Error",
                detail,
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }
}
