package com.example.LMS_BE.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.LMS_BE.common.util.MessageUtils;
import com.example.LMS_BE.course.controller.CourseController;
import com.example.LMS_BE.course.dto.CourseRequest;
import com.example.LMS_BE.course.service.CourseService;
import com.example.LMS_BE.lesson.dto.LessonRequest;
import com.example.LMS_BE.student.controller.StudentController;
import com.example.LMS_BE.student.dto.StudentRequest;
import com.example.LMS_BE.student.service.StudentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class RequestValidationTest {
  private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
  private static final Validator VALIDATOR = FACTORY.getValidator();

  @AfterAll
  static void closeValidatorFactory() {
    FACTORY.close();
  }

  @Test
  void studentSearchRejectsNegativePageAndNonPositiveSize() throws Exception {
    StudentController controller =
        new StudentController(mock(StudentService.class), mock(MessageUtils.class));
    Method method = StudentController.class.getMethod("search", String.class, int.class, int.class);

    Set<ConstraintViolation<StudentController>> violations =
        VALIDATOR
            .forExecutables()
            .validateParameters(controller, method, new Object[] {null, -1, 0});

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .anyMatch(path -> path.endsWith(".page"))
        .anyMatch(path -> path.endsWith(".size"));
  }

  @Test
  void courseSearchRejectsNegativePageAndNonPositiveSize() throws Exception {
    CourseController controller =
        new CourseController(mock(CourseService.class), mock(MessageUtils.class));
    Method method =
        CourseController.class.getMethod(
            "search", String.class, LocalDate.class, LocalDate.class, int.class, int.class);

    Set<ConstraintViolation<CourseController>> violations =
        VALIDATOR
            .forExecutables()
            .validateParameters(controller, method, new Object[] {null, null, null, -1, 0});

    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .anyMatch(path -> path.endsWith(".page"))
        .anyMatch(path -> path.endsWith(".size"));
  }

  @Test
  void mediaChangeListsRejectNullAndNonPositiveIds() {
    StudentRequest student = new StudentRequest();
    student.setRetainedMediaIds(List.of(0L));
    student.setNewMediaIds(java.util.Arrays.asList((Long) null));
    student.setRemovedMediaIds(List.of(-1L));

    CourseRequest course = new CourseRequest();
    course.setRetainedMediaIds(List.of(0L));
    course.setNewMediaIds(java.util.Arrays.asList((Long) null));
    course.setRemovedMediaIds(List.of(-1L));

    LessonRequest lesson = new LessonRequest();
    lesson.setRetainedMediaIds(List.of(0L));
    lesson.setNewMediaIds(java.util.Arrays.asList((Long) null));
    lesson.setRemovedMediaIds(List.of(-1L));

    assertMediaListViolations(VALIDATOR.validate(student));
    assertMediaListViolations(VALIDATOR.validate(course));
    assertMediaListViolations(VALIDATOR.validate(lesson));
  }

  @Test
  void studentPhoneAcceptsBlankAndSupportedFormatsButRejectsInvalidText() {
    StudentRequest student = new StudentRequest();
    student.setStudentCode("SV-001");
    student.setFullName("Nguyen Van A");
    student.setEmail("student@example.com");

    student.setPhone("");
    assertThat(VALIDATOR.validate(student))
        .noneMatch(v -> v.getPropertyPath().toString().equals("phone"));

    student.setPhone("0901234567");
    assertThat(VALIDATOR.validate(student))
        .noneMatch(v -> v.getPropertyPath().toString().equals("phone"));

    student.setPhone("+84901234567");
    assertThat(VALIDATOR.validate(student))
        .noneMatch(v -> v.getPropertyPath().toString().equals("phone"));

    student.setPhone("09A-123-456");
    assertThat(VALIDATOR.validate(student))
        .anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
  }

  private void assertMediaListViolations(Set<? extends ConstraintViolation<?>> violations) {
    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .anyMatch(path -> path.startsWith("retainedMediaIds"))
        .anyMatch(path -> path.startsWith("newMediaIds"))
        .anyMatch(path -> path.startsWith("removedMediaIds"));
  }
}
