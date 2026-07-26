package com.example.LMS_BE.common.exception;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Converts database-specific integrity errors into stable API error codes. Database messages are
 * only used for classification and are never returned directly to the client.
 */
final class DatabaseConstraintResolver {

  private static final Pattern[] CONSTRAINT_PATTERNS = {
    Pattern.compile("(?i)for\\s+key\\s+['\"`]([^'\"`]+)['\"`]"),
    Pattern.compile("(?i)constraint\\s+['\"`]([^'\"`]+)['\"`]"),
    Pattern.compile("(?i)key\\s*\\(([^)]+)\\)\\s*="),
    Pattern.compile("(?i)(?:unique\\s+)?index[^\\r\\n]*?\\bon\\s+[^()]+\\(([^)]+)\\)")
  };

  private static final Pattern[] COLUMN_PATTERNS = {
    Pattern.compile("(?i)column\\s+['\"`]([^'\"`]+)['\"`]"),
    Pattern.compile("(?i)field\\s+['\"`]([^'\"`]+)['\"`]"),
    Pattern.compile("(?i)null\\s+value\\s+in\\s+column\\s+['\"`]([^'\"`]+)['\"`]"),
    Pattern.compile("(?i)foreign\\s+key\\s*\\(([^)]+)\\)"),
    Pattern.compile("(?i)key\\s*\\(([^)]+)\\)\\s*="),
    Pattern.compile("(?i)not-null\\s+property[^:]*:\\s*[\\w.]+\\.([a-zA-Z][a-zA-Z0-9_]*)")
  };

  Resolution resolve(DataIntegrityViolationException exception) {
    Diagnostic diagnostic = Diagnostic.from(exception);

    if (diagnostic.isDuplicate()) {
      return resolveDuplicate(diagnostic.constraintIdentifier());
    }
    if (diagnostic.isNotNull()) {
      return new Resolution(
          ErrorCode.DATABASE_REQUIRED_FIELD, fieldMessageKey(diagnostic.columnIdentifier()));
    }
    if (diagnostic.isValueTooLong()) {
      return new Resolution(
          ErrorCode.DATABASE_VALUE_TOO_LONG, fieldMessageKey(diagnostic.columnIdentifier()));
    }
    if (diagnostic.isMissingReference()) {
      return resolveMissingReference(diagnostic.referenceIdentifier());
    }
    if (diagnostic.isResourceInUse()) {
      return new Resolution(ErrorCode.DATABASE_RESOURCE_IN_USE, null);
    }
    if (diagnostic.isCheckConstraint()) {
      String identifier = diagnostic.constraintIdentifier();
      if (containsAny(identifier, "progress_percent", "progresspercent", "progress")) {
        return new Resolution(ErrorCode.ENROLLMENT_INVALID_PROGRESS, null);
      }
      return new Resolution(ErrorCode.DATABASE_CHECK_CONSTRAINT, null);
    }
    if (diagnostic.isInvalidValue()) {
      return new Resolution(
          ErrorCode.DATABASE_INVALID_VALUE, fieldMessageKey(diagnostic.columnIdentifier()));
    }

    return new Resolution(ErrorCode.DATABASE_CONSTRAINT, null);
  }

  private Resolution resolveDuplicate(String identifier) {
    if (containsAny(identifier, "email")) {
      return new Resolution(ErrorCode.STUDENT_DUPLICATE_EMAIL, null);
    }
    if (containsAny(identifier, "student_code", "studentcode")) {
      return new Resolution(ErrorCode.STUDENT_DUPLICATE_CODE, null);
    }
    if (containsAny(identifier, "course_code", "coursecode")) {
      return new Resolution(ErrorCode.COURSE_DUPLICATE_CODE, null);
    }
    if (containsAny(identifier, "lesson_code", "lessoncode")) {
      return new Resolution(ErrorCode.LESSON_DUPLICATE_CODE, null);
    }
    if (containsAll(identifier, "student", "course")
        || containsAny(identifier, "enrollment", "enrolment")) {
      return new Resolution(ErrorCode.ENROLLMENT_DUPLICATE_RELATION, null);
    }
    if (containsAny(identifier, "object_media", "objectmedia")
        || containsAll(identifier, "object", "media")) {
      return new Resolution(ErrorCode.MEDIA_DUPLICATE_LINK, null);
    }
    return new Resolution(ErrorCode.DATABASE_DUPLICATE_DATA, null);
  }

  private Resolution resolveMissingReference(String identifier) {
    if (containsAny(identifier, "student_id", "studentid", "student")) {
      return new Resolution(ErrorCode.STUDENT_NOT_FOUND, null);
    }
    if (containsAny(identifier, "course_id", "courseid", "course")) {
      return new Resolution(ErrorCode.COURSE_NOT_FOUND, null);
    }
    if (containsAny(identifier, "media_id", "mediaid", "media")) {
      return new Resolution(ErrorCode.MEDIA_NOT_FOUND, null);
    }
    return new Resolution(ErrorCode.DATABASE_RELATED_DATA_NOT_FOUND, null);
  }

  private String fieldMessageKey(String column) {
    String normalized = normalizeIdentifier(column);
    return switch (normalized) {
      case "student_code", "studentcode" -> "field.studentCode";
      case "full_name", "fullname" -> "field.fullName";
      case "email" -> "field.email";
      case "phone" -> "field.phone";
      case "date_of_birth", "dateofbirth" -> "field.dateOfBirth";
      case "gender" -> "field.gender";
      case "address" -> "field.address";
      case "course_code", "coursecode" -> "field.courseCode";
      case "course_name", "coursename" -> "field.courseName";
      case "price" -> "field.price";
      case "start_date", "startdate" -> "field.startDate";
      case "end_date", "enddate" -> "field.endDate";
      case "course_id", "courseid" -> "field.courseId";
      case "lesson_code", "lessoncode" -> "field.lessonCode";
      case "title" -> "field.title";
      case "lesson_order", "lessonorder" -> "field.lessonOrder";
      case "duration_seconds", "durationseconds" -> "field.durationSeconds";
      case "student_id", "studentid" -> "field.studentId";
      case "enrolled_at", "enrolledat" -> "field.enrolledAt";
      case "enrollment_status", "enrollmentstatus" -> "field.enrollmentStatus";
      case "progress_percent", "progresspercent" -> "field.progressPercent";
      case "completed_at", "completedat" -> "field.completedAt";
      case "object_type", "objecttype" -> "field.objectType";
      case "object_id", "objectid" -> "field.objectId";
      case "media_id", "mediaid" -> "field.mediaId";
      case "media_type", "mediatype" -> "field.mediaType";
      case "display_order", "displayorder" -> "field.displayOrder";
      case "is_primary", "isprimary" -> "field.isPrimary";
      case "original_name", "originalname" -> "field.originalName";
      case "stored_name", "storedname" -> "field.storedName";
      case "stored_path", "storedpath" -> "field.storedPath";
      case "mime_type", "mimetype" -> "field.mimeType";
      case "file_size", "filesize" -> "field.fileSize";
      case "status" -> "field.status";
      case "created_at" -> "field.createdAt";
      case "updated_at" -> "field.updatedAt";
      default -> "field.data";
    };
  }

  private static boolean containsAny(String source, String... values) {
    String normalized = normalizeText(source);
    for (String value : values) {
      if (normalized.contains(value)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsAll(String source, String... values) {
    String normalized = normalizeText(source);
    for (String value : values) {
      if (!normalized.contains(value)) {
        return false;
      }
    }
    return true;
  }

  private static String normalizeIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    String normalized =
        value.toLowerCase(Locale.ROOT).replace("`", "").replace("\"", "").replace("'", "").trim();
    int comma = normalized.indexOf(',');
    if (comma >= 0) {
      normalized = normalized.substring(0, comma).trim();
    }
    int dot = normalized.lastIndexOf('.');
    return dot >= 0 ? normalized.substring(dot + 1).trim() : normalized;
  }

  private static String normalizeText(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  static final class Resolution {
    private final ErrorCode errorCode;
    private final String fieldMessageKey;

    Resolution(ErrorCode errorCode, String fieldMessageKey) {
      this.errorCode = errorCode;
      this.fieldMessageKey = fieldMessageKey;
    }

    ErrorCode getErrorCode() {
      return errorCode;
    }

    String getFieldMessageKey() {
      return fieldMessageKey;
    }
  }

  private static final class Diagnostic {
    private final String text;
    private final String sqlState;
    private final int vendorCode;
    private final String hibernateConstraint;

    private Diagnostic(String text, String sqlState, int vendorCode, String hibernateConstraint) {
      this.text = text;
      this.sqlState = sqlState;
      this.vendorCode = vendorCode;
      this.hibernateConstraint = hibernateConstraint;
    }

    static Diagnostic from(Throwable throwable) {
      StringBuilder text = new StringBuilder();
      String state = null;
      int code = 0;
      String constraint = null;
      Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
      Throwable current = throwable;

      while (current != null && visited.add(current)) {
        if (current.getMessage() != null) {
          text.append(' ').append(current.getMessage());
        }
        if (current instanceof SQLException sqlException) {
          if (state == null) {
            state = sqlException.getSQLState();
          }
          if (code == 0) {
            code = sqlException.getErrorCode();
          }
        }
        if (current
                instanceof org.hibernate.exception.ConstraintViolationException constraintException
            && constraintException.getConstraintName() != null) {
          constraint = constraintException.getConstraintName();
        }
        current = current.getCause();
      }
      return new Diagnostic(normalizeText(text.toString()), state, code, constraint);
    }

    boolean isDuplicate() {
      return vendorCode == 1062
          || "23505".equals(sqlState)
          || text.contains("duplicate entry")
          || text.contains("duplicate key")
          || text.contains("unique constraint")
          || text.contains("unique index or primary key violation");
    }

    boolean isNotNull() {
      return vendorCode == 1048
          || vendorCode == 1364
          || "23502".equals(sqlState)
          || text.contains("cannot be null")
          || text.contains("null value in column")
          || text.contains("doesn't have a default value")
          || text.contains("null not allowed")
          || text.contains("not-null property references a null");
    }

    boolean isValueTooLong() {
      return vendorCode == 1406
          || "22001".equals(sqlState)
          || text.contains("data too long for column")
          || text.contains("value too long for type")
          || text.contains("value too long for column");
    }

    boolean isMissingReference() {
      return vendorCode == 1452
          || "23503".equals(sqlState) && !isResourceInUse()
          || text.contains("cannot add or update a child row")
          || text.contains("is not present in table");
    }

    boolean isResourceInUse() {
      return vendorCode == 1451
          || text.contains("cannot delete or update a parent row")
          || text.contains("is still referenced from table");
    }

    boolean isCheckConstraint() {
      return vendorCode == 3819
          || vendorCode == 4025
          || "23514".equals(sqlState)
          || text.contains("check constraint") && text.contains("violat");
    }

    boolean isInvalidValue() {
      return vendorCode == 1264
          || vendorCode == 1265
          || vendorCode == 1366
          || "22003".equals(sqlState)
          || "22007".equals(sqlState)
          || text.contains("out of range value for column")
          || text.contains("data truncated for column")
          || text.contains("incorrect integer value")
          || text.contains("incorrect decimal value")
          || text.contains("incorrect date value");
    }

    String constraintIdentifier() {
      String extracted = extract(text, CONSTRAINT_PATTERNS);
      return (hibernateConstraint == null ? "" : hibernateConstraint) + " " + extracted;
    }

    String columnIdentifier() {
      return extract(text, COLUMN_PATTERNS);
    }

    String referenceIdentifier() {
      String constraint = constraintIdentifier();
      String column = columnIdentifier();
      return constraint + " " + column;
    }

    private static String extract(String source, Pattern[] patterns) {
      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
          return matcher.group(1);
        }
      }
      return "";
    }
  }
}
