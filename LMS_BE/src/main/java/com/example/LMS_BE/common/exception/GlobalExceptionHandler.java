package com.example.LMS_BE.common.exception;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
  private final MessageUtils messages;
  private final DatabaseConstraintResolver databaseConstraints = new DatabaseConstraintResolver();

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException e) {
    return business(e);
  }

  @ExceptionHandler(DuplicateDataException.class)
  ResponseEntity<ApiResponse<Void>> duplicate(DuplicateDataException e) {
    return business(e);
  }

  @ExceptionHandler(FileStorageException.class)
  ResponseEntity<ApiResponse<Void>> storage(FileStorageException e) {
    return business(e);
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<ApiResponse<Void>> businessError(BusinessException e) {
    return business(e);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
    return validationResponse(e.getBindingResult());
  }

  @ExceptionHandler(BindException.class)
  ResponseEntity<ApiResponse<Void>> bindValidation(BindException e) {
    return validationResponse(e.getBindingResult());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<ApiResponse<Void>> methodValidation(HandlerMethodValidationException e) {
    List<String> details = new ArrayList<>();
    for (ParameterValidationResult result : e.getParameterValidationResults()) {
      if (result instanceof ParameterErrors parameterErrors) {
        details.addAll(
            parameterErrors.getFieldErrors().stream().map(this::formatFieldError).toList());
        continue;
      }

      String field = methodParameterName(result.getMethodParameter());
      for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
        details.add(localizedField(field) + ": " + validationMessage(resolvable));
      }
    }
    e.getCrossParameterValidationResults().stream()
        .map(this::validationMessage)
        .forEach(details::add);

    String message =
        details.stream()
            .filter(detail -> !detail.isBlank())
            .distinct()
            .collect(Collectors.joining("; "));
    return response(
        ErrorCode.VALIDATION_ERROR,
        message.isBlank() ? messages.get(ErrorCode.VALIDATION_ERROR.getMessageKey()) : message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> constraint(ConstraintViolationException e) {
    String message =
        e.getConstraintViolations().stream()
            .map(
                v -> {
                  String path = v.getPropertyPath().toString();
                  int lastDot = path.lastIndexOf('.');
                  String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
                  return localizedField(field) + ": " + v.getMessage();
                })
            .distinct()
            .collect(Collectors.joining("; "));
    return response(ErrorCode.CONSTRAINT_VIOLATION, message);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
    return error(ErrorCode.FILE_TOO_LARGE);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e) {
    return response(ErrorCode.TYPE_MISMATCH, typeMismatchMessage(e.getName(), e.getRequiredType()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiResponse<Void>> malformed(HttpMessageNotReadableException e) {
    MismatchedInputException mismatch = findCause(e, MismatchedInputException.class);
    if (mismatch == null) {
      return error(ErrorCode.INVALID_REQUEST);
    }

    String field = jacksonField(mismatch);
    return response(ErrorCode.TYPE_MISMATCH, typeMismatchMessage(field, mismatch.getTargetType()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  ResponseEntity<ApiResponse<Void>> missingParameter(MissingServletRequestParameterException e) {
    return response(
        ErrorCode.MISSING_PARAMETER,
        messages.get(
            ErrorCode.MISSING_PARAMETER.getMessageKey(), localizedField(e.getParameterName())));
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  ResponseEntity<ApiResponse<Void>> missingPart(MissingServletRequestPartException e) {
    return response(
        ErrorCode.MISSING_REQUEST_PART,
        messages.get(
            ErrorCode.MISSING_REQUEST_PART.getMessageKey(),
            localizedField(e.getRequestPartName())));
  }

  @ExceptionHandler(ServletRequestBindingException.class)
  ResponseEntity<ApiResponse<Void>> requestBinding(ServletRequestBindingException e) {
    return error(ErrorCode.REQUEST_BINDING_ERROR);
  }

  @ExceptionHandler(MultipartException.class)
  ResponseEntity<ApiResponse<Void>> multipart(MultipartException e) {
    return error(ErrorCode.INVALID_MULTIPART_REQUEST);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ResponseEntity<ApiResponse<Void>> mediaType(HttpMediaTypeNotSupportedException e) {
    return error(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  ResponseEntity<ApiResponse<Void>> notAcceptable(HttpMediaTypeNotAcceptableException e) {
    return error(ErrorCode.NOT_ACCEPTABLE);
  }

  @ExceptionHandler(HttpMessageNotWritableException.class)
  ResponseEntity<ApiResponse<Void>> responseWrite(HttpMessageNotWritableException e) {
    log.error("Failed to serialize the HTTP response", e);
    return error(ErrorCode.RESPONSE_WRITE_ERROR);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e) {
    DatabaseConstraintResolver.Resolution resolution = databaseConstraints.resolve(e);
    ErrorCode code = resolution.getErrorCode();
    log.warn("Database integrity violation mapped to error code {}", code.name());
    if (resolution.getFieldMessageKey() != null) {
      String fieldName = messages.get(resolution.getFieldMessageKey());
      return response(code, messages.get(code.getMessageKey(), fieldName));
    }
    return error(code);
  }

  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  ResponseEntity<ApiResponse<Void>> apiNotFound(Exception e) {
    return error(ErrorCode.API_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiResponse<Void>> methodNotAllowed(HttpRequestMethodNotSupportedException e) {
    return error(ErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unknown(Exception e) {
    log.error("Unhandled application error", e);
    return error(ErrorCode.UNCATEGORIZED_EXCEPTION);
  }

  private ResponseEntity<ApiResponse<Void>> validationResponse(BindingResult bindingResult) {
    List<String> details = new ArrayList<>();
    bindingResult.getFieldErrors().stream().map(this::formatFieldError).forEach(details::add);
    bindingResult.getGlobalErrors().stream().map(this::validationMessage).forEach(details::add);

    String message =
        details.stream()
            .filter(detail -> !detail.isBlank())
            .distinct()
            .collect(Collectors.joining("; "));
    return response(
        ErrorCode.VALIDATION_ERROR,
        message.isBlank() ? messages.get(ErrorCode.VALIDATION_ERROR.getMessageKey()) : message);
  }

  private String formatFieldError(FieldError error) {
    if (hasCode(error, "typeMismatch")) {
      return typeMismatchMessage(error.getField(), inferRequiredType(error.getField()));
    }
    String constraint = error.getCode() == null ? "invalid" : error.getCode();
    return localizedField(error.getField())
        + ": "
        + messages.get("validation." + constraint, error.getRejectedValue());
  }

  private String validationMessage(MessageSourceResolvable error) {
    String code =
        error.getCodes() == null || error.getCodes().length == 0
            ? null
            : error.getCodes()[error.getCodes().length - 1];
    if (code != null) {
      String localized = messages.get("validation." + code, error.getArguments());
      if (!localized.equals("validation." + code)) {
        return localized;
      }
    }
    return error.getDefaultMessage() == null
        ? messages.get(ErrorCode.VALIDATION_ERROR.getMessageKey())
        : error.getDefaultMessage();
  }

  private boolean hasCode(FieldError error, String prefix) {
    if (error.getCodes() == null) {
      return false;
    }
    for (String code : error.getCodes()) {
      if (code != null && code.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private String typeMismatchMessage(String field, Class<?> requiredType) {
    String expectedType = expectedTypeMessage(field, requiredType);
    return messages.get(
        ErrorCode.TYPE_MISMATCH.getMessageKey(), localizedField(field), expectedType);
  }

  private String expectedTypeMessage(String field, Class<?> requiredType) {
    if (requiredType != null && requiredType.isEnum()) {
      String values =
          Arrays.stream(requiredType.getEnumConstants())
              .map(value -> ((Enum<?>) value).name())
              .collect(Collectors.joining(", "));
      return messages.get("validation.type.enum_values", values);
    }
    return messages.get(typeMessageKey(field, requiredType));
  }

  private String jacksonField(JacksonException exception) {
    String field = "data";
    for (JacksonException.Reference reference : exception.getPath()) {
      if (reference.getPropertyName() != null && !reference.getPropertyName().isBlank()) {
        field = reference.getPropertyName();
      }
    }
    return field;
  }

  private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < 20; depth++) {
      if (type.isInstance(current)) {
        return type.cast(current);
      }
      Throwable next = current.getCause();
      if (next == current) {
        break;
      }
      current = next;
    }
    return null;
  }

  private String typeMessageKey(String field, Class<?> requiredType) {
    String normalized = baseField(field);
    if (DATE_FIELDS.contains(normalized) || isDateType(requiredType)) {
      return "validation.type.date";
    }
    if (DATETIME_FIELDS.contains(normalized) || isDateTimeType(requiredType)) {
      return "validation.type.datetime";
    }
    if (DECIMAL_FIELDS.contains(normalized) || isDecimalType(requiredType)) {
      return "validation.type.decimal";
    }
    if (INTEGER_FIELDS.contains(normalized) || isIntegerType(requiredType)) {
      return "validation.type.integer";
    }
    if (requiredType == Boolean.class || requiredType == boolean.class) {
      return "validation.type.boolean";
    }
    if (requiredType != null && requiredType.isEnum()) {
      return "validation.type.enum";
    }
    if (requiredType != null && Number.class.isAssignableFrom(requiredType)) {
      return "validation.type.number";
    }
    return "validation.type.value";
  }

  private Class<?> inferRequiredType(String field) {
    String normalized = baseField(field);
    if (DATE_FIELDS.contains(normalized)) return LocalDate.class;
    if (DATETIME_FIELDS.contains(normalized)) return LocalDateTime.class;
    if (DECIMAL_FIELDS.contains(normalized)) return BigDecimal.class;
    if (INTEGER_FIELDS.contains(normalized)) return Long.class;
    return null;
  }

  private String localizedField(String field) {
    String normalized = baseField(field);
    String key = "field." + normalized;
    String localized = messages.get(key);
    return localized.equals(key)
        ? (field == null || field.isBlank() ? messages.get("field.data") : field)
        : localized;
  }

  private String baseField(String field) {
    if (field == null || field.isBlank()) return "data";
    int lastDot = field.lastIndexOf('.');
    String normalized = lastDot >= 0 ? field.substring(lastDot + 1) : field;
    int bracket = normalized.indexOf('[');
    return bracket >= 0 ? normalized.substring(0, bracket) : normalized;
  }

  private String methodParameterName(MethodParameter parameter) {
    RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
    if (requestParam != null) {
      String name = !requestParam.name().isBlank() ? requestParam.name() : requestParam.value();
      if (!name.isBlank()) return name;
    }
    PathVariable pathVariable = parameter.getParameterAnnotation(PathVariable.class);
    if (pathVariable != null) {
      String name = !pathVariable.name().isBlank() ? pathVariable.name() : pathVariable.value();
      if (!name.isBlank()) return name;
    }
    return parameter.getParameterName() == null ? "data" : parameter.getParameterName();
  }

  private boolean isDateType(Class<?> type) {
    return type == LocalDate.class
        || type == Year.class
        || type == YearMonth.class
        || type == MonthDay.class;
  }

  private boolean isDateTimeType(Class<?> type) {
    return type == LocalDateTime.class
        || type == OffsetDateTime.class
        || type == ZonedDateTime.class
        || type == Instant.class;
  }

  private boolean isDecimalType(Class<?> type) {
    return type == BigDecimal.class
        || type == Double.class
        || type == double.class
        || type == Float.class
        || type == float.class;
  }

  private boolean isIntegerType(Class<?> type) {
    return type == BigInteger.class
        || type == Byte.class
        || type == byte.class
        || type == Short.class
        || type == short.class
        || type == Integer.class
        || type == int.class
        || type == Long.class
        || type == long.class;
  }

  private static final Set<String> DATE_FIELDS = Set.of("dateOfBirth", "startDate", "endDate");
  private static final Set<String> DATETIME_FIELDS =
      Set.of("enrolledAt", "completedAt", "createdAt", "updatedAt", "createdFrom", "createdTo");
  private static final Set<String> DECIMAL_FIELDS = Set.of("price", "progressPercent");
  private static final Set<String> INTEGER_FIELDS =
      Set.of(
          "id",
          "page",
          "size",
          "status",
          "studentId",
          "courseId",
          "lessonId",
          "enrollmentId",
          "mediaId",
          "objectId",
          "courseIds",
          "retainedMediaIds",
          "newMediaIds",
          "removedMediaIds",
          "lessonOrder",
          "durationSeconds",
          "displayOrder",
          "fileSize",
          "isPrimary");

  private ResponseEntity<ApiResponse<Void>> business(BusinessException e) {
    return response(
        e.getErrorCode(), messages.get(e.getErrorCode().getMessageKey(), e.getArguments()));
  }

  private ResponseEntity<ApiResponse<Void>> error(ErrorCode code) {
    return response(code, messages.get(code.getMessageKey()));
  }

  private ResponseEntity<ApiResponse<Void>> response(ErrorCode code, String message) {
    return ResponseEntity.status(code.getHttpStatus())
        .body(ApiResponse.error(code.name(), message));
  }
}
