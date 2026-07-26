package com.example.LMS_BE.common.exception;

public class DuplicateDataException extends BusinessException {
  public DuplicateDataException(ErrorCode errorCode, Object... arguments) {
    super(errorCode, arguments);
  }
}
