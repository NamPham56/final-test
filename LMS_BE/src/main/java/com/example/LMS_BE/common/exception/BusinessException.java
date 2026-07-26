package com.example.LMS_BE.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;
  private final Object[] arguments;

  public BusinessException(ErrorCode errorCode, Object... arguments) {
    super(errorCode.name());
    this.errorCode = errorCode;
    this.arguments = arguments;
  }
}
