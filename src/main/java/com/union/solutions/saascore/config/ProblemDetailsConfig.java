package com.union.solutions.saascore.config;

import com.union.solutions.saascore.adapters.in.rest.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProblemDetailsConfig {

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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetails> handleOther(HttpServletRequest req) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ProblemDetails.of(
                500,
                "Internal Server Error",
                "An error occurred",
                req.getRequestURI(),
                TenantContext.getCorrelationId()));
  }
}
