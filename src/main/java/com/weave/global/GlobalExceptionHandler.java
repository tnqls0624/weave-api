package com.weave.global;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex,
      HttpServletRequest req) {
    var code = ex.getErrorCode();
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), null, ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidArgs(MethodArgumentNotValidException ex,
      HttpServletRequest req) {
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
            (a, b) -> a
        ));

    var code = ErrorCode.VALIDATION_ERROR;
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), fieldErrors, code.getMessage()));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex,
      HttpServletRequest req) {
    var code = ErrorCode.VALIDATION_ERROR;
    Map<String, String> errors = ex.getConstraintViolations().stream()
        .collect(Collectors.toMap(
            v -> v.getPropertyPath().toString(),
            ConstraintViolation::getMessage,
            (a, b) -> a
        ));
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), errors, code.getMessage()));
  }

  // Spring Security/DB 예외를 도메인 에러코드로 매핑
  @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(RuntimeException ex,
      HttpServletRequest req) {
    var code = ErrorCode.INVALID_LOGIN;
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), null, code.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
      HttpServletRequest req) {
    // 유니크 키(예: username) 충돌 시
    var code = ErrorCode.DUPLICATE_EMAIL;
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), null, code.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleEtc(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
    var code = ErrorCode.INTERNAL_ERROR;
    return ResponseEntity
        .status(code.getHttpStatus())
        .body(ApiErrorResponse.of(code, req.getRequestURI(), null, code.getMessage()));
  }

  // --- 표준 에러 응답 DTO ---
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record ApiErrorResponse(
      String code,
      String message,
      String path,
      Instant timestamp,
      Map<String, String> errors
  ) {

    public static ApiErrorResponse of(ErrorCode errorCode, String path,
        Map<String, String> errors, String overrideMessage) {
      return new ApiErrorResponse(
          errorCode.getCode(),
          (overrideMessage != null ? overrideMessage : errorCode.getMessage()),
          path,
          Instant.now(),
          errors
      );
    }
  }
}