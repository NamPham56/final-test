package com.example.LMS_BE.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.LMS_BE.common.dto.ApiResponse;
import com.example.LMS_BE.common.util.MessageUtils;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messageUtils());

  @AfterEach
  void resetLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void mapsCourseIdTypeMismatchToLocalizedIntegerMessage() throws Exception {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
    Method method =
        Parameters.class.getDeclaredMethod("search", Long.class, LocalDate.class, BigDecimal.class);
    MethodParameter parameter = new MethodParameter(method, 0);
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "abc",
            Long.class,
            "courseId",
            parameter,
            new IllegalArgumentException("invalid number"));

    ResponseEntity<ApiResponse<Void>> response = handler.typeMismatch(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
    assertThat(response.getBody().getMessage())
        .isEqualTo("Khóa học không đúng kiểu dữ liệu; yêu cầu giá trị dạng số nguyên");
  }

  @Test
  void mapsDateAndDecimalTypesToSpecificEnglishMessages() throws Exception {
    LocaleContextHolder.setLocale(Locale.ENGLISH);
    Method method =
        Parameters.class.getDeclaredMethod("search", Long.class, LocalDate.class, BigDecimal.class);

    ResponseEntity<ApiResponse<Void>> dateResponse =
        handler.typeMismatch(
            new MethodArgumentTypeMismatchException(
                "15/07/2026", LocalDate.class, "startDate", new MethodParameter(method, 1), null));
    ResponseEntity<ApiResponse<Void>> decimalResponse =
        handler.typeMismatch(
            new MethodArgumentTypeMismatchException(
                "free", BigDecimal.class, "price", new MethodParameter(method, 2), null));

    assertThat(dateResponse.getBody().getMessage()).contains("Start date", "yyyy-MM-dd");
    assertThat(decimalResponse.getBody().getMessage()).contains("Price", "decimal number");
  }

  @Test
  void handlesBindExceptionWithFieldValidationDetails() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
    BindException exception = new BindException(new Object(), "studentRequest");
    exception.addError(
        new FieldError(
            "studentRequest",
            "email",
            "invalid-email",
            false,
            new String[] {"Email.studentRequest.email", "Email.email", "Email"},
            null,
            "must be a well-formed email address"));

    ResponseEntity<ApiResponse<Void>> response = handler.bindValidation(exception);

    assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
    assertThat(response.getBody().getMessage()).isEqualTo("Email: không đúng định dạng email");
  }

  @Test
  void mapsMalformedMultipartAndMissingHandlerSeparately() {
    ResponseEntity<ApiResponse<Void>> multipart =
        handler.multipart(new MultipartException("missing boundary"));
    ResponseEntity<ApiResponse<Void>> missingApi =
        handler.apiNotFound(new NoHandlerFoundException("GET", "/api/unknown", HttpHeaders.EMPTY));

    assertThat(multipart.getBody().getCode()).isEqualTo("INVALID_MULTIPART_REQUEST");
    assertThat(missingApi.getStatusCode().value()).isEqualTo(404);
    assertThat(missingApi.getBody().getCode()).isEqualTo("API_NOT_FOUND");
  }

  @Test
  void reportsMissingParameterAndMultipartPartNames() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    ResponseEntity<ApiResponse<Void>> parameter =
        handler.missingParameter(new MissingServletRequestParameterException("courseId", "long"));
    ResponseEntity<ApiResponse<Void>> part =
        handler.missingPart(new MissingServletRequestPartException("avatar"));

    assertThat(parameter.getBody().getCode()).isEqualTo("MISSING_PARAMETER");
    assertThat(parameter.getBody().getMessage())
        .isEqualTo("Missing required request parameter: Course");
    assertThat(part.getBody().getCode()).isEqualTo("MISSING_REQUEST_PART");
    assertThat(part.getBody().getMessage()).isEqualTo("Missing required multipart part: avatar");
  }

  @Test
  void reportsJacksonEnumDateAndNumberTypesWithoutEchoingRejectedPayload() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    InvalidFormatException invalidEnum =
        InvalidFormatException.from(
            null, "invalid enum", "secret-invalid-payload", EnrollmentState.class);
    invalidEnum.prependPath(new Object(), "enrollmentStatus");
    InvalidFormatException invalidDate =
        InvalidFormatException.from(null, "invalid date", "private-date-value", LocalDate.class);
    invalidDate.prependPath(new Object(), "startDate");
    MismatchedInputException invalidNumber =
        MismatchedInputException.from(null, Long.class, "invalid number");
    invalidNumber.prependPath(new Object(), "lessonOrder");

    ResponseEntity<ApiResponse<Void>> enumResponse = handler.malformed(readException(invalidEnum));
    ResponseEntity<ApiResponse<Void>> dateResponse = handler.malformed(readException(invalidDate));
    ResponseEntity<ApiResponse<Void>> numberResponse =
        handler.malformed(readException(invalidNumber));

    assertThat(enumResponse.getBody().getCode()).isEqualTo("TYPE_MISMATCH");
    assertThat(enumResponse.getBody().getMessage())
        .contains("Enrollment status", "ACTIVE, PAUSED")
        .doesNotContain("secret-invalid-payload");
    assertThat(dateResponse.getBody().getMessage())
        .contains("Start date", "yyyy-MM-dd")
        .doesNotContain("private-date-value");
    assertThat(numberResponse.getBody().getMessage()).contains("Lesson order", "integer");
  }

  @Test
  void keepsMalformedJsonGenericAndMapsAdditionalSpringMvcFailures() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    ResponseEntity<ApiResponse<Void>> malformed =
        handler.malformed(
            new HttpMessageNotReadableException("raw JSON must not be returned", inputMessage()));
    ResponseEntity<ApiResponse<Void>> notAcceptable =
        handler.notAcceptable(new HttpMediaTypeNotAcceptableException("not acceptable"));
    ResponseEntity<ApiResponse<Void>> binding =
        handler.requestBinding(new ServletRequestBindingException("sensitive binding detail"));
    ResponseEntity<ApiResponse<Void>> write =
        handler.responseWrite(new HttpMessageNotWritableException("serialization failed"));

    assertThat(malformed.getBody().getCode()).isEqualTo("INVALID_REQUEST");
    assertThat(malformed.getBody().getMessage()).doesNotContain("raw JSON");
    assertThat(notAcceptable.getStatusCode().value()).isEqualTo(406);
    assertThat(notAcceptable.getBody().getCode()).isEqualTo("NOT_ACCEPTABLE");
    assertThat(binding.getStatusCode().value()).isEqualTo(400);
    assertThat(binding.getBody().getCode()).isEqualTo("REQUEST_BINDING_ERROR");
    assertThat(binding.getBody().getMessage()).doesNotContain("sensitive binding detail");
    assertThat(write.getStatusCode().value()).isEqualTo(500);
    assertThat(write.getBody().getCode()).isEqualTo("RESPONSE_WRITE_ERROR");
  }

  private static HttpMessageNotReadableException readException(Throwable cause) {
    return new HttpMessageNotReadableException("JSON conversion failed", cause, inputMessage());
  }

  private static MockHttpInputMessage inputMessage() {
    return new MockHttpInputMessage(new byte[0]);
  }

  private static MessageUtils messageUtils() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("messages");
    source.setDefaultEncoding("UTF-8");
    return new MessageUtils(source);
  }

  private static class Parameters {
    @SuppressWarnings("unused")
    void search(Long courseId, LocalDate startDate, BigDecimal price) {}
  }

  private enum EnrollmentState {
    ACTIVE,
    PAUSED
  }
}
