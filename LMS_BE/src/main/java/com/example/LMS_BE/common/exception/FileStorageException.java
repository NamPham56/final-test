package com.example.LMS_BE.common.exception;

public class FileStorageException extends BusinessException {
  public FileStorageException(ErrorCode errorCode, Throwable cause, Object... arguments) {
    super(errorCode, arguments);
    initCause(cause);
  }
}
