package br.com.financialcontrol.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return build(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Dados inválidos.",
        request.getRequestURI(),
        fields);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_ERROR",
        "Dados inválidos.",
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiError> handleUnauthorized(
      UnauthorizedException exception, HttpServletRequest request) {
    return build(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        exception.getMessage(),
        request.getRequestURI(),
        null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(
      ConflictException exception, HttpServletRequest request) {
    return build(
        HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), request.getRequestURI(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(
      Exception exception, HttpServletRequest request) {
    log.error("Unexpected error at {}", request.getRequestURI(), exception);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Não foi possível concluir a operação.",
        request.getRequestURI(),
        null);
  }

  private static ResponseEntity<ApiError> build(
      HttpStatus status, String code, String message, String path, Map<String, String> fields) {
    ApiError body = new ApiError(Instant.now(), status.value(), code, message, path, fields);
    return ResponseEntity.status(status).body(body);
  }
}
