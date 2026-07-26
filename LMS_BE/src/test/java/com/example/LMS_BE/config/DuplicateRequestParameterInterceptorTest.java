package com.example.LMS_BE.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.LMS_BE.common.exception.BusinessException;
import com.example.LMS_BE.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DuplicateRequestParameterInterceptorTest {

  private final DuplicateRequestParameterInterceptor interceptor =
      new DuplicateRequestParameterInterceptor();

  @Test
  void acceptsSingleValueParameters() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
    request.addParameter("page", "0");

    boolean accepted = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    assertThat(accepted).isTrue();
  }

  @Test
  void rejectsRepeatedScalarParameterWithItsName() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
    request.addParameter("page", "0", "1");

    assertThatThrownBy(
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception -> {
              BusinessException business = (BusinessException) exception;
              assertThat(business.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PARAMETER);
              assertThat(business.getArguments()).containsExactly("page");
            });
  }
}
