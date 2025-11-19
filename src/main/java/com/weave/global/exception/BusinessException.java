package com.weave.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detail;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detail = null;
  }

  public BusinessException(ErrorCode errorCode, String detail) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.detail = detail;
  }

  public BusinessException(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.detail = null;
  }

  public BusinessException(String message, ErrorCode errorCode, String detail) {
    super(message);
    this.errorCode = errorCode;
    this.detail = detail;
  }
}