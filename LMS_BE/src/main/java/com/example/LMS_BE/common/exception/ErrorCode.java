package com.example.LMS_BE.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  UNCATEGORIZED_EXCEPTION("error.uncategorized", HttpStatus.INTERNAL_SERVER_ERROR),
  VALIDATION_ERROR("error.validation", HttpStatus.BAD_REQUEST),
  CONSTRAINT_VIOLATION("error.constraint_violation", HttpStatus.BAD_REQUEST),
  TYPE_MISMATCH("error.type_mismatch", HttpStatus.BAD_REQUEST),
  INVALID_REQUEST("error.invalid_request", HttpStatus.BAD_REQUEST),
  API_NOT_FOUND("error.api_not_found", HttpStatus.NOT_FOUND),
  METHOD_NOT_ALLOWED("error.method_not_allowed", HttpStatus.METHOD_NOT_ALLOWED),
  NOT_ACCEPTABLE("error.not_acceptable", HttpStatus.NOT_ACCEPTABLE),
  UNSUPPORTED_MEDIA_TYPE("error.unsupported_media_type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  REQUEST_BINDING_ERROR("error.request_binding", HttpStatus.BAD_REQUEST),
  RESPONSE_WRITE_ERROR("error.response_write", HttpStatus.INTERNAL_SERVER_ERROR),
  MISSING_PARAMETER("error.missing_parameter", HttpStatus.BAD_REQUEST),
  MISSING_REQUEST_PART("error.missing_request_part", HttpStatus.BAD_REQUEST),
  DUPLICATE_PARAMETER("error.duplicate_parameter", HttpStatus.BAD_REQUEST),
  INVALID_MULTIPART_REQUEST("error.invalid_multipart_request", HttpStatus.BAD_REQUEST),
  FILE_TOO_LARGE("file.tooLarge", HttpStatus.CONTENT_TOO_LARGE),
  FILE_STORAGE_ERROR("file.storageError", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_PHYSICAL_MISSING("file.physicalMissing", HttpStatus.NOT_FOUND),
  EXCEL_EXPORT_FAILED("excel.export.failed", HttpStatus.INTERNAL_SERVER_ERROR),
  DATABASE_DUPLICATE_DATA("database.duplicateData", HttpStatus.CONFLICT),
  DATABASE_REQUIRED_FIELD("database.requiredField", HttpStatus.BAD_REQUEST),
  DATABASE_VALUE_TOO_LONG("database.valueTooLong", HttpStatus.BAD_REQUEST),
  DATABASE_INVALID_VALUE("database.invalidValue", HttpStatus.BAD_REQUEST),
  DATABASE_RELATED_DATA_NOT_FOUND("database.relatedDataNotFound", HttpStatus.BAD_REQUEST),
  DATABASE_RESOURCE_IN_USE("database.resourceInUse", HttpStatus.CONFLICT),
  DATABASE_CHECK_CONSTRAINT("database.checkConstraint", HttpStatus.BAD_REQUEST),
  DATABASE_CONSTRAINT("database.constraint", HttpStatus.CONFLICT),

  STUDENT_NOT_FOUND("student.notFound", HttpStatus.NOT_FOUND),
  STUDENT_DUPLICATE_CODE("student.duplicateCode", HttpStatus.CONFLICT),
  STUDENT_DUPLICATE_EMAIL("student.duplicateEmail", HttpStatus.CONFLICT),
  STUDENT_EXPORT_EMPTY("student.export.empty", HttpStatus.NOT_FOUND),

  COURSE_NOT_FOUND("course.notFound", HttpStatus.NOT_FOUND),
  COURSE_DUPLICATE_CODE("course.duplicateCode", HttpStatus.CONFLICT),
  COURSE_INVALID_PRICE("course.invalidPrice", HttpStatus.BAD_REQUEST),
  COURSE_INVALID_DATE_RANGE("course.invalidDateRange", HttpStatus.BAD_REQUEST),
  COURSE_HAS_ACTIVE_ENROLLMENTS("course.hasActiveEnrollments", HttpStatus.CONFLICT),
  COURSE_EXPORT_EMPTY("course.export.empty", HttpStatus.NOT_FOUND),

  LESSON_NOT_FOUND("lesson.notFound", HttpStatus.NOT_FOUND),
  LESSON_DUPLICATE_CODE("lesson.duplicateCode", HttpStatus.CONFLICT),
  LESSON_INVALID_ORDER("lesson.invalidOrder", HttpStatus.BAD_REQUEST),
  LESSON_INVALID_DURATION("lesson.invalidDuration", HttpStatus.BAD_REQUEST),

  ENROLLMENT_NOT_FOUND("enrollment.notFound", HttpStatus.NOT_FOUND),
  ENROLLMENT_DUPLICATE("enrollment.duplicate", HttpStatus.CONFLICT),
  ENROLLMENT_DUPLICATE_COURSE_REQUEST("enrollment.duplicateCourseRequest", HttpStatus.BAD_REQUEST),
  ENROLLMENT_DUPLICATE_RELATION("enrollment.duplicateRelation", HttpStatus.CONFLICT),
  ENROLLMENT_INVALID_PROGRESS("enrollment.invalidProgress", HttpStatus.BAD_REQUEST),
  ENROLLMENT_COURSE_UNAVAILABLE("enrollment.courseUnavailable", HttpStatus.NOT_FOUND),
  ENROLLMENT_COURSE_UNAVAILABLE_WITH_ID("enrollment.courseUnavailableWithId", HttpStatus.NOT_FOUND),

  MEDIA_NOT_FOUND("media.notFound", HttpStatus.NOT_FOUND),
  MEDIA_NOT_FOUND_WITH_ID("media.notFoundWithId", HttpStatus.NOT_FOUND),
  MEDIA_DUPLICATE_LINK("media.duplicateLink", HttpStatus.CONFLICT),
  MEDIA_CONFLICTING_CHANGES("media.conflictingChanges", HttpStatus.BAD_REQUEST),
  MEDIA_RETAINED_NOT_LINKED("media.retainedNotLinked", HttpStatus.BAD_REQUEST),
  MEDIA_REMOVED_NOT_LINKED("media.removedNotLinked", HttpStatus.BAD_REQUEST),
  FILE_INVALID_NAME("file.invalidName", HttpStatus.BAD_REQUEST),
  FILE_INVALID_MIME_TYPE("file.invalidMimeType", HttpStatus.BAD_REQUEST),
  FILE_INVALID_SIZE("file.invalidSize", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_INVALID_PATH("file.invalidPath", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_EMPTY("file.empty", HttpStatus.BAD_REQUEST);

  private final String messageKey;
  private final HttpStatus httpStatus;

  ErrorCode(String messageKey, HttpStatus httpStatus) {
    this.messageKey = messageKey;
    this.httpStatus = httpStatus;
  }
}
