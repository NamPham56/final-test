package com.example.LMS_BE.common.exception;

public class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(ErrorCode errorCode, Object... arguments) {
    super(errorCode, arguments);
  }
}
