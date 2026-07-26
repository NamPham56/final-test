package com.example.LMS_BE.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DatabaseConstraintResolverTest {

  private final DatabaseConstraintResolver resolver = new DatabaseConstraintResolver();

  @Test
  void mapsDuplicateEmailToSpecificStudentError() {
    DatabaseConstraintResolver.Resolution result =
        resolve("Duplicate entry 'student@example.com' for key 'students.email'", "23000", 1062);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.STUDENT_DUPLICATE_EMAIL);
  }

  @Test
  void mapsDuplicateActiveEnrollmentToSpecificError() {
    DatabaseConstraintResolver.Resolution result =
        resolve("Duplicate entry '1-2' for key 'uk_enrollment_student_course'", "23000", 1062);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ENROLLMENT_DUPLICATE_RELATION);
  }

  @Test
  void mapsNotNullAndKeepsTheLocalizedFieldKey() {
    DatabaseConstraintResolver.Resolution result =
        resolve("Column 'full_name' cannot be null", "23000", 1048);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.DATABASE_REQUIRED_FIELD);
    assertThat(result.getFieldMessageKey()).isEqualTo("field.fullName");
  }

  @Test
  void mapsMissingCourseForeignKeyToCourseNotFound() {
    DatabaseConstraintResolver.Resolution result =
        resolve(
            "Cannot add or update a child row: a foreign key constraint fails "
                + "CONSTRAINT `fk_enrollment_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)",
            "23000",
            1452);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.COURSE_NOT_FOUND);
  }

  @Test
  void mapsValueTooLongAndKeepsTheLocalizedFieldKey() {
    DatabaseConstraintResolver.Resolution result =
        resolveSql("Data too long for column 'phone' at row 1", "22001", 1406);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.DATABASE_VALUE_TOO_LONG);
    assertThat(result.getFieldMessageKey()).isEqualTo("field.phone");
  }

  @Test
  void mapsProgressCheckConstraintToSpecificError() {
    DatabaseConstraintResolver.Resolution result =
        resolve("Check constraint 'chk_progress_percent' is violated", "23000", 3819);

    assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ENROLLMENT_INVALID_PROGRESS);
  }

  private DatabaseConstraintResolver.Resolution resolve(
      String message, String state, int vendorCode) {
    return resolver.resolve(
        new DataIntegrityViolationException(
            "Database write failed",
            new SQLIntegrityConstraintViolationException(message, state, vendorCode)));
  }

  private DatabaseConstraintResolver.Resolution resolveSql(
      String message, String state, int vendorCode) {
    return resolver.resolve(
        new DataIntegrityViolationException(
            "Database write failed", new SQLException(message, state, vendorCode)));
  }
}
